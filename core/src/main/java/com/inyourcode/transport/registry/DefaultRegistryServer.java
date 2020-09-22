/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inyourcode.transport.registry;

import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.JUnsafe;
import com.inyourcode.common.util.Lists;
import com.inyourcode.common.util.Pair;
import com.inyourcode.common.util.Signal;
import com.inyourcode.common.util.Strings;
import com.inyourcode.common.util.SystemClock;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.serialization.api.SerializerType;
import com.inyourcode.transport.api.Acknowledge;
import com.inyourcode.transport.api.JConfig;
import com.inyourcode.transport.api.JOption;
import com.inyourcode.transport.api.JProtocolHeader;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.exception.IoSignals;
import com.inyourcode.transport.netty.NettyTcpAcceptor;
import com.inyourcode.transport.netty.TcpChannelProvider;
import com.inyourcode.transport.netty.channel.NettyChannel;
import com.inyourcode.transport.netty.handler.AcknowledgeEncoder;
import com.inyourcode.transport.netty.handler.IdleStateChecker;
import com.inyourcode.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;
import com.inyourcode.transport.registry.api.RegisterMeta;
import com.inyourcode.transport.registry.api.RegistryServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;


/**
 * The server of registration center.
 *
 * 所有信息均在内存中, 不持久化.
 *
 * provider(client)断线时所有该provider发布过的服务会被server清除并通知订阅者, 重新建立连接后provider会自动重新发布相关服务,
 * 并且server会重新推送服务给订阅者.
 *
 * consumer(client)断线时所有该consumer订阅过的服务会被server清除, 重新建立连接后consumer会自动重新订阅相关服务.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class DefaultRegistryServer extends NettyTcpAcceptor implements RegistryServer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRegistryServer.class);

    private static final AttributeKey<ConcurrentSet<RegisterMeta.ServiceMeta>> S_SUBSCRIBE_KEY =
            AttributeKey.valueOf("server.subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> S_PUBLISH_KEY =
            AttributeKey.valueOf("server.published");

    // 注册信息
    private final RegisterInfoContext registerInfoContext = new RegisterInfoContext();
    // 订阅者
    private final ChannelGroup subscriberChannels = new DefaultChannelGroup("subscribers", GlobalEventExecutor.INSTANCE);
    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<String, MessageNonAck> messagesNonAck = new ConcurrentHashMap<>();

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();

    public DefaultRegistryServer(int port) {
        super(port, false);
    }

    public DefaultRegistryServer(SocketAddress address) {
        super(address, false);
    }

    public DefaultRegistryServer(int port, int nWorks) {
        super(port, nWorks, false);
    }

    public DefaultRegistryServer(SocketAddress address, int nWorks) {
        super(address, nWorks, false);
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 1024);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        ServerBootstrap boot = bootstrap();

        boot.channelFactory(TcpChannelProvider.NIO_ACCEPTOR)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new IdleStateChecker(timer, JConstants.READER_IDLE_TIME_SECONDS, 0, 0),
                                idleStateTrigger,
                                new MessageDecoder(),
                                encoder,
                                ackEncoder,
                                handler);
                    }
                });

        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    public List<String> listPublisherHosts() {
        List<RegisterMeta.Address> fromList = registerInfoContext.listPublisherHosts();

        return Lists.transform(fromList, new Function<RegisterMeta.Address, String>() {

            @Override
            public String apply(RegisterMeta.Address input) {
                return input.getHost();
            }
        });
    }

    @Override
    public List<String> listSubscriberAddresses() {
        List<String> hosts = Lists.newArrayList();
        for (Channel ch : subscriberChannels) {
            SocketAddress address = ch.remoteAddress();
            if (address instanceof InetSocketAddress) {
                String host = ((InetSocketAddress) address).getAddress().getHostAddress();
                int port = ((InetSocketAddress) address).getPort();
                hosts.add(new RegisterMeta.Address(host, port).toString());
            }
        }
        return hosts;
    }

    @Override
    public List<String> listAddressesByService(String group, String serviceProviderName, String version) {
        RegisterMeta.ServiceMeta serviceMeta = new RegisterMeta.ServiceMeta(group, serviceProviderName, version);
        List<RegisterMeta.Address> fromList = registerInfoContext.listAddressesByService(serviceMeta);

        return Lists.transform(fromList, new Function<RegisterMeta.Address, String>() {

            @Override
            public String apply(RegisterMeta.Address input) {
                return input.toString();
            }
        });
    }

    @Override
    public List<String> listServicesByAddress(String host, int port) {
        RegisterMeta.Address address = new RegisterMeta.Address(host, port);
        List<RegisterMeta.ServiceMeta> fromList = registerInfoContext.listServicesByAddress(address);

        return Lists.transform(fromList, new Function<RegisterMeta.ServiceMeta, String>() {

            @Override
            public String apply(RegisterMeta.ServiceMeta input) {
                return input.toString();
            }
        });
    }

    @Override
    public void startRegistryServer() {
        try {
            start();
        } catch (InterruptedException e) {
            JUnsafe.throwException(e);
        }
    }

    // 添加指定机器指定服务, 然后全量发布到所有客户端
    private void handlePublish(RegisterMeta meta, Channel channel) {

        logger.info("Publish {} on channel{}.", meta, channel);

        attachPublishEventOnChannel(meta, channel);

        final RegisterMeta.ServiceMeta serviceMeta = meta.getServiceMeta();
        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            if (config.getConfig().putIfAbsent(meta.getAddress(), meta) == null) {
                registerInfoContext.getServiceMeta(meta.getAddress()).add(serviceMeta);

                final Message msg = new Message(SerializerType.PROTO_STUFF.value());
                msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
                msg.version(config.newVersion()); // 版本号+1
                msg.data(Pair.of(serviceMeta, meta));

                subscriberChannels.writeAndFlush(msg, new ChannelMatcher() {

                    @Override
                    public boolean matches(Channel channel) {
                        boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, channel);
                        if (doSend) {
                            MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, channel);
                            // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                            messagesNonAck.put(msgNonAck.id, msgNonAck);
                        }
                        return doSend;
                    }
                });
            }
        }
    }

    // 删除指定机器指定服务, 然后全量发布到所有客户端
    private void handlePublishCancel(RegisterMeta meta, Channel channel) {

        logger.info("Cancel publish {} on channel{}.", meta, channel);

        attachPublishCancelEventOnChannel(meta, channel);

        final RegisterMeta.ServiceMeta serviceMeta = meta.getServiceMeta();
        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            RegisterMeta.Address address = meta.getAddress();
            RegisterMeta data = config.getConfig().remove(address);
            if (data != null) {
                registerInfoContext.getServiceMeta(address).remove(serviceMeta);

                final Message msg = new Message(SerializerType.PROTO_STUFF.value());
                msg.messageCode(JProtocolHeader.PUBLISH_CANCEL_SERVICE);
                msg.version(config.newVersion()); // 版本号+1
                msg.data(Pair.of(serviceMeta, data));

                subscriberChannels.writeAndFlush(msg, new ChannelMatcher() {

                    @Override
                    public boolean matches(Channel channel) {
                        boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, channel);
                        if (doSend) {
                            MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, channel);
                            // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                            messagesNonAck.put(msgNonAck.id, msgNonAck);
                        }
                        return doSend;
                    }
                });
            }
        }
    }

    // 订阅服务
    private void handleSubscribe(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {

        logger.info("Subscribe {} on channel{}.", serviceMeta, channel);

        attachSubscribeEventOnChannel(serviceMeta, channel);

        subscriberChannels.add(channel);

        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        final Message msg = new Message(SerializerType.PROTO_STUFF.value());
        msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
        msg.version(config.getVersion()); // 版本号
        List<RegisterMeta> registerMetaList = Lists.newArrayList(config.getConfig().values());
        // 每次发布服务都是当前meta的全量信息
        msg.data(Pair.of(serviceMeta, registerMetaList));

        MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, channel);
        // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
        messagesNonAck.put(msgNonAck.id, msgNonAck);
        channel.writeAndFlush(msg);
    }

    // 处理ack
    private void handleAcknowledge(Acknowledge ack, Channel channel) {
        messagesNonAck.remove(key(ack.sequence(), channel));
    }

    // 发布Provider下线的通告
    private void handleOfflineNotice(RegisterMeta.Address address) {

        logger.info("OfflineNotice on {}.", address);

        Message msg = new Message(SerializerType.PROTO_STUFF.value());
        msg.messageCode(JProtocolHeader.OFFLINE_NOTICE);
        msg.data(address);
        subscriberChannels.writeAndFlush(msg);
    }

    private static String key(long sequence, Channel channel) {
        return String.valueOf(sequence) + '-' + channel.id().asShortText();
    }

    // 在channel打标记(发布过的服务)
    private static boolean attachPublishEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(S_PUBLISH_KEY);
        ConcurrentSet<RegisterMeta> registerMetaSet = attr.get();
        if (registerMetaSet == null) {
            ConcurrentSet<RegisterMeta> newRegisterMetaSet = new ConcurrentSet<>();
            registerMetaSet = attr.setIfAbsent(newRegisterMetaSet);
            if (registerMetaSet == null) {
                registerMetaSet = newRegisterMetaSet;
            }
        }

        return registerMetaSet.add(meta);
    }

    // 取消在channel的标记(发布过的服务)
    private static boolean attachPublishCancelEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(S_PUBLISH_KEY);
        ConcurrentSet<RegisterMeta> registerMetaSet = attr.get();
        if (registerMetaSet == null) {
            ConcurrentSet<RegisterMeta> newRegisterMetaSet = new ConcurrentSet<>();
            registerMetaSet = attr.setIfAbsent(newRegisterMetaSet);
            if (registerMetaSet == null) {
                registerMetaSet = newRegisterMetaSet;
            }
        }

        return registerMetaSet.remove(meta);
    }

    // 在channel打标记(订阅过的服务)
    private static boolean attachSubscribeEventOnChannel(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta.ServiceMeta>> attr = channel.attr(S_SUBSCRIBE_KEY);
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = attr.get();
        if (serviceMetaSet == null) {
            ConcurrentSet<RegisterMeta.ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
            serviceMetaSet = attr.setIfAbsent(newServiceMetaSet);
            if (serviceMetaSet == null) {
                serviceMetaSet = newServiceMetaSet;
            }
        }

        return serviceMetaSet.add(serviceMeta);
    }

    // 检查channel上的标记(是否订阅过指定的服务)
    private static boolean isChannelSubscribeOnServiceMeta(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = channel.attr(S_SUBSCRIBE_KEY).get();

        return serviceMetaSet != null && serviceMetaSet.contains(serviceMeta);
    }

    /**
     * 没收到ACK, 需要重发消息
     */
    static class MessageNonAck {
        private final String id;

        private final RegisterMeta.ServiceMeta serviceMeta;
        private final Message msg;
        private final Channel channel;
        private final long version;
        private final long timestamp = SystemClock.millisClock().now();

        public MessageNonAck(RegisterMeta.ServiceMeta serviceMeta, Message msg, Channel channel) {
            this.serviceMeta = serviceMeta;
            this.msg = msg;
            this.channel = channel;
            this.version = msg.version();

            id = key(msg.sequence(), channel);
        }
    }

    /**
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
     * + 1 // 空
     * + 8 // 消息 id, long 类型
     * + 4 // 消息体 body 长度, int类型
     */
    static class MessageDecoder extends ReplayingDecoder<MessageDecoder.State> {

        public MessageDecoder() {
            super(State.HEADER_MAGIC);
        }

        // 协议头
        private final JProtocolHeader header = new JProtocolHeader();

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            switch (state()) {
                case HEADER_MAGIC:
                    checkMagic(in.readShort());             // MAGIC
                    checkpoint(State.HEADER_SIGN);
                case HEADER_SIGN:
                    header.sign(in.readByte());             // 消息标志位
                    checkpoint(State.HEADER_STATUS);
                case HEADER_STATUS:
                    in.readByte();                          // no-op
                    checkpoint(State.HEADER_ID);
                case HEADER_ID:
                    header.id(in.readLong());               // 消息id
                    checkpoint(State.HEADER_BODY_LENGTH);
                case HEADER_BODY_LENGTH:
                    header.bodyLength(in.readInt());        // 消息体长度
                    checkpoint(State.BODY);
                case BODY:
                    byte s_code = header.serializerCode();

                    switch (header.messageCode()) {
                        case JProtocolHeader.HEARTBEAT:
                            break;
                        case JProtocolHeader.PUBLISH_SERVICE:
                        case JProtocolHeader.PUBLISH_CANCEL_SERVICE:
                        case JProtocolHeader.SUBSCRIBE_SERVICE:
                        case JProtocolHeader.OFFLINE_NOTICE: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Serializer serializer = SerializerFactory.getSerializer(s_code);
                            Message msg = serializer.readObject(bytes, Message.class);
                            msg.messageCode(header.messageCode());
                            out.add(msg);

                            break;
                        }
                        case JProtocolHeader.ACK:
                            out.add(new Acknowledge(header.id()));

                            break;
                        default:
                            throw IoSignals.ILLEGAL_SIGN;
                    }
                    checkpoint(State.HEADER_MAGIC);
            }
        }

        private static void checkMagic(short magic) throws Signal {
            if (magic != JProtocolHeader.MAGIC) {
                throw IoSignals.ILLEGAL_MAGIC;
            }
        }

        enum State {
            HEADER_MAGIC,
            HEADER_SIGN,
            HEADER_STATUS,
            HEADER_ID,
            HEADER_BODY_LENGTH,
            BODY
        }
    }

    /**
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
     * + 1 // 空
     * + 8 // 消息 id, long 类型
     * + 4 // 消息体 body 长度, int类型
     */
    @ChannelHandler.Sharable
    static class MessageEncoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
            byte s_code = msg.serializerCode();
            byte sign = (byte) ((s_code << 4) + msg.messageCode());
            Serializer serializer = SerializerFactory.getSerializer(s_code);
            byte[] bytes = serializer.writeObject(msg);

            out.writeShort(JProtocolHeader.MAGIC)
                    .writeByte(sign)
                    .writeByte(0)
                    .writeLong(0)
                    .writeInt(bytes.length)
                    .writeBytes(bytes);
        }
    }

    @ChannelHandler.Sharable
    class MessageHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel channel = ctx.channel();

            if (msg instanceof Message) {
                Message obj = (Message) msg;

                switch (obj.messageCode()) {
                    case JProtocolHeader.PUBLISH_SERVICE:
                    case JProtocolHeader.PUBLISH_CANCEL_SERVICE:
                        RegisterMeta meta = (RegisterMeta) obj.data();
                        if (Strings.isNullOrEmpty(meta.getHost())) {
                            SocketAddress address = channel.remoteAddress();
                            if (address instanceof InetSocketAddress) {
                                meta.setHost(((InetSocketAddress) address).getAddress().getHostAddress());
                            } else {
                                logger.warn("Could not get remote host: {}, info: {}", channel, meta);

                                return;
                            }
                        }

                        if (obj.messageCode() == JProtocolHeader.PUBLISH_SERVICE) {
                            handlePublish(meta, channel);
                        } else if (obj.messageCode() == JProtocolHeader.PUBLISH_CANCEL_SERVICE) {
                            handlePublishCancel(meta, channel);
                        }
                        channel.writeAndFlush(new Acknowledge(obj.sequence())) // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        break;
                    case JProtocolHeader.SUBSCRIBE_SERVICE:
                        handleSubscribe((RegisterMeta.ServiceMeta) obj.data(), channel);
                        channel.writeAndFlush(new Acknowledge(obj.sequence())) // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        break;
                    case JProtocolHeader.OFFLINE_NOTICE:
                        handleOfflineNotice((RegisterMeta.Address) obj.data());

                        break;
                }
            } else if (msg instanceof Acknowledge) {
                handleAcknowledge((Acknowledge) msg, channel);
            } else {
                logger.warn("Unexpected msg type received:{}.", msg.getClass());

                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();

            // 取消之前发布的所有服务
            ConcurrentSet<RegisterMeta> registerMetaSet = channel.attr(S_PUBLISH_KEY).get();

            if (registerMetaSet == null || registerMetaSet.isEmpty()) {
                return;
            }

            RegisterMeta.Address address = null;
            for (RegisterMeta meta : registerMetaSet) {
                if (address == null) {
                    address = meta.getAddress();
                }
                handlePublishCancel(meta, channel);
            }

            if (address != null) {
                // 通知所有订阅者对应机器下线
                handleOfflineNotice(address);
            }
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            Channel ch = ctx.channel();

            // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
            // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
            if (!ch.isWritable()) {
                // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
                logger.warn("{} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, ch.config().getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());

                ch.config().setAutoRead(false);
            } else {
                // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
                logger.warn("{} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, ch.config().getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());

                ch.config().setAutoRead(true);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
            if (cause instanceof Signal) {
                IoSignals.handleSignal((Signal) cause, jChannel);
            } else {
                logger.error("An exception has been caught {}, on {}.", stackTrace(cause), jChannel);
            }
        }
    }

    @SuppressWarnings("all")
    private class AckTimeoutScanner implements Runnable {

        @Override
        public void run() {
            for (;;) {
                try {
                    for (MessageNonAck m : messagesNonAck.values()) {
                        if (SystemClock.millisClock().now() - m.timestamp > TimeUnit.SECONDS.toMillis(10)) {

                            // 移除
                            if (messagesNonAck.remove(m.id) == null) {
                                continue;
                            }

                            if (registerInfoContext.getRegisterMeta(m.serviceMeta).getVersion() > m.version) {
                                // 旧版本的内容不需要重发
                                continue;
                            }

                            if (m.channel.isActive()) {
                                MessageNonAck msgNonAck = new MessageNonAck(m.serviceMeta, m.msg, m.channel);
                                messagesNonAck.put(msgNonAck.id, msgNonAck);
                                m.channel.writeAndFlush(m.msg)
                                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                            }
                        }
                    }

                    Thread.sleep(300);
                } catch (Throwable t) {
                    logger.error("An exception has been caught while scanning the timeout acknowledges {}.", stackTrace(t));
                }
            }
        }
    }

    {
        Thread t = new Thread(new AckTimeoutScanner(), "ack.timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}

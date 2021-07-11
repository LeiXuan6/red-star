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
import com.inyourcode.common.util.Pair;
import com.inyourcode.common.util.Signal;
import com.inyourcode.common.util.SystemClock;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.serialization.api.SerializerType;
import com.inyourcode.transport.api.Acknowledge;
import com.inyourcode.transport.api.JConnection;
import com.inyourcode.transport.api.JOption;
import com.inyourcode.transport.api.JProtocolHeader;
import com.inyourcode.transport.api.UnresolvedAddress;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.exception.ConnectFailedException;
import com.inyourcode.transport.api.exception.IoSignals;
import com.inyourcode.transport.netty.NettyTcpConnector;
import com.inyourcode.transport.netty.TcpChannelProvider;
import com.inyourcode.transport.netty.channel.NettyChannel;
import com.inyourcode.transport.netty.handler.AcknowledgeEncoder;
import com.inyourcode.transport.netty.handler.IdleStateChecker;
import com.inyourcode.transport.netty.handler.connector.ConnectionWatchdog;
import com.inyourcode.transport.netty.handler.connector.ConnectorIdleStateTrigger;
import com.inyourcode.transport.registry.api.AbstractRegistryService;
import com.inyourcode.transport.registry.api.NotifyListener;
import com.inyourcode.transport.registry.api.RegisterMeta;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.inyourcode.common.util.Preconditions.checkNotNull;
import static com.inyourcode.common.util.StackTraceUtil.stackTrace;


/**
 * The client of registration center.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class DefaultRegistry extends NettyTcpConnector {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRegistry.class);

    private static final AttributeKey<ConcurrentSet<RegisterMeta.ServiceMeta>> C_SUBSCRIBE_KEY =
            AttributeKey.valueOf("client.subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> C_PUBLISH_KEY =
            AttributeKey.valueOf("client.published");

    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<Long, MessageNonAck> messagesNonAck = new ConcurrentHashMap<>();

    // handlers
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();

    // 每个ConfigClient只保留一个有效channel
    private volatile Channel channel;

    private AbstractRegistryService registryService;

    public DefaultRegistry(AbstractRegistryService registryService) {
        this(registryService, 1);
    }

    public DefaultRegistry(AbstractRegistryService registryService, int nWorkers) {
        super(nWorkers);
        this.registryService = checkNotNull(registryService, "registryService");
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(3));
        // channel factory
        bootstrap().channelFactory(TcpChannelProvider.NIO_CONNECTOR);
    }

    /**
     * ConfigClient不支持异步连接行为, async参数无效
     */
    @Override
    public JConnection connect(UnresolvedAddress address, boolean async) {
        setOptions();

        final Bootstrap boot = bootstrap();
        final SocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());

        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, socketAddress, null) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new IdleStateChecker(timer, 0, JConstants.WRITER_IDLE_TIME_SECONDS, 0),
                        idleStateTrigger,
                        new MessageDecoder(),
                        encoder,
                        ackEncoder,
                        handler
                };
            }};
        watchdog.start();

        try {
            ChannelFuture future;
            synchronized (bootstrapLock()) {
                boot.handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect(socketAddress);
            }

            // 以下代码在synchronized同步块外面是安全的
            future.sync();
            channel = future.channel();
        } catch (Throwable t) {
            throw new ConnectFailedException("connects to [" + address + "] fails", t);
        }

        return new JConnection(address) {

            @Override
            public void setReconnect(boolean reconnect) {
                if (reconnect) {
                    watchdog.start();
                } else {
                    watchdog.stop();
                }
            }
        };
    }

    /**
     * Sent the subscription information to registry server.
     */
    public void doSubscribe(RegisterMeta.ServiceMeta serviceMeta) {
        registryService.subscribeSet().add(serviceMeta);

        Message msg = new Message(SerializerType.PROTO_STUFF.value());
        msg.messageCode(JProtocolHeader.SUBSCRIBE_SERVICE);
        msg.data(serviceMeta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachSubscribeEventOnChannel(serviceMeta, ch)) {
            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    /**
     * Publishing service to registry server.
     */
    public void doRegister(RegisterMeta meta) {
        registryService.registerMetaSet().add(meta);

        Message msg = new Message(SerializerType.PROTO_STUFF.value());
        msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
        msg.data(meta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachPublishEventOnChannel(meta, ch)) {
            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    /**
     * Notify to registry server unpublish corresponding service.
     */
    public void doUnregister(final RegisterMeta meta) {
        registryService.registerMetaSet().remove(meta);

        Message msg = new Message(SerializerType.PROTO_STUFF.value());
        msg.messageCode(JProtocolHeader.PUBLISH_CANCEL_SERVICE);
        msg.data(meta);

        channel.writeAndFlush(msg)
                .addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            Channel ch = future.channel();
                            if (ch.isActive()) {
                                ch.pipeline().fireExceptionCaught(future.cause());
                            } else {
                                logger.warn("Unregister {} fail because of channel is inactive: {}.", meta, stackTrace(future.cause()));
                            }
                        }
                    }
                });

        MessageNonAck msgNonAck = new MessageNonAck(msg, channel);
        messagesNonAck.put(msgNonAck.id, msgNonAck);
    }

    private void handleAcknowledge(Acknowledge ack) {
        messagesNonAck.remove(ack.sequence());
    }

    // 在channel打标记(发布过的服务)
    private static boolean attachPublishEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(C_PUBLISH_KEY);
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

    // 在channel打标记(订阅过的服务)
    private static boolean attachSubscribeEventOnChannel(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta.ServiceMeta>> attr = channel.attr(C_SUBSCRIBE_KEY);
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

    static class MessageNonAck {
        private final long id;

        private final Message msg;
        private final Channel channel;
        private final long timestamp = SystemClock.millisClock().now();

        public MessageNonAck(Message msg, Channel channel) {
            this.msg = msg;
            this.channel = channel;

            id = msg.sequence();
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
                        case JProtocolHeader.PUBLISH_SERVICE:
                        case JProtocolHeader.PUBLISH_CANCEL_SERVICE:
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

        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Message) {
                Message obj = (Message) msg;

                switch (obj.messageCode()) {
                    case JProtocolHeader.PUBLISH_SERVICE: {
                        Pair<RegisterMeta.ServiceMeta, ?> data = (Pair<RegisterMeta.ServiceMeta, ?>) obj.data();
                        Object metaObj = data.getValue();

                        if (metaObj instanceof List) {
                            List<RegisterMeta> list = (List<RegisterMeta>) metaObj;
                            RegisterMeta[] array = new RegisterMeta[list.size()];
                            list.toArray(array);
                            registryService.notify(
                                    data.getKey(),
                                    NotifyListener.NotifyEvent.CHILD_ADDED,
                                    obj.version(),
                                    array
                            );
                        } else if (metaObj instanceof RegisterMeta) {
                            registryService.notify(
                                    data.getKey(),
                                    NotifyListener.NotifyEvent.CHILD_ADDED,
                                    obj.version(),
                                    (RegisterMeta) metaObj
                            );
                        }

                        ctx.channel()
                                .writeAndFlush(new Acknowledge(obj.sequence()))  // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        logger.info("Publish from RegistryServer {}, metadata : {}, version: {}.",
                                data.getKey(), metaObj, obj.version());

                        break;
                    }
                    case JProtocolHeader.PUBLISH_CANCEL_SERVICE: {
                        Pair<RegisterMeta.ServiceMeta, RegisterMeta> data =
                                (Pair<RegisterMeta.ServiceMeta, RegisterMeta>) obj.data();
                        registryService.notify(
                                data.getKey(), NotifyListener.NotifyEvent.CHILD_REMOVED, obj.version(), data.getValue());

                        ctx.channel()
                                .writeAndFlush(new Acknowledge(obj.sequence()))  // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        logger.info("Publish cancel from RegistryServer {}, metadata : {}, version: {}.",
                                data.getKey(), data.getValue(), obj.version());

                        break;
                    }
                    case JProtocolHeader.OFFLINE_NOTICE:
                        RegisterMeta.Address address = (RegisterMeta.Address) obj.data();

                        logger.info("Offline notice on {}.", address);

                        registryService.offline(address);

                        break;
                }
            } else if (msg instanceof Acknowledge) {
                handleAcknowledge((Acknowledge) msg);
            } else {
                logger.warn("Unexpected msg type received: {}.", msg.getClass());

                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel ch = (channel = ctx.channel());

            // 重新订阅
            for (RegisterMeta.ServiceMeta serviceMeta : registryService.subscribeSet()) {
                // 与doSubscribe()中的write有竞争
                if (!attachSubscribeEventOnChannel(serviceMeta, ch)) {
                    continue;
                }

                Message msg = new Message(SerializerType.PROTO_STUFF.value());
                msg.messageCode(JProtocolHeader.SUBSCRIBE_SERVICE);
                msg.data(serviceMeta);

                ch.writeAndFlush(msg)
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
                messagesNonAck.put(msgNonAck.id, msgNonAck);
            }

            // 重新发布服务
            for (RegisterMeta meta : registryService.registerMetaSet()) {
                // 与doRegister()中的write有竞争
                if (!attachPublishEventOnChannel(meta, ch)) {
                    continue;
                }

                Message msg = new Message(SerializerType.PROTO_STUFF.value());
                msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
                msg.data(meta);

                ch.writeAndFlush(msg)
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
                messagesNonAck.put(msgNonAck.id, msgNonAck);
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

    private class AckTimeoutScanner implements Runnable {

        @SuppressWarnings("InfiniteLoopStatement")
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

                            if (m.channel.isActive()) {
                                MessageNonAck msgNonAck = new MessageNonAck(m.msg, m.channel);
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

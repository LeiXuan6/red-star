/*
 * Copyright (c) 2020 The red-star Project
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
package com.inyourcode.cluster;

import com.inyourcode.cluster.api.ClusterForwardHandler;
import com.inyourcode.cluster.api.ClusterMessageHandler;
import com.inyourcode.cluster.api.IClusterNodeType;
import com.inyourcode.cluster.api.JClusterClient;
import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.JServiceLoader;
import com.inyourcode.common.util.Signal;
import com.inyourcode.common.util.Strings;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.serialization.api.SerializerType;
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
import com.inyourcode.transport.netty.channel.NettyChannelGroup;
import com.inyourcode.transport.netty.handler.IdleStateChecker;
import com.inyourcode.transport.netty.handler.connector.ConnectionWatchdog;
import com.inyourcode.transport.netty.handler.connector.ConnectorIdleStateTrigger;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.inyourcode.common.util.Preconditions.checkNotNull;
import static com.inyourcode.common.util.StackTraceUtil.stackTrace;

/**
 * @author JackLei
 */
public class ClusterNodeClient extends NettyTcpConnector implements JClusterClient {
    private static final Logger logger = LoggerFactory.getLogger(ClusterNodeClient.class);
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private ConnectionWatchdog watchdog;
    private ClusterNodeConf conf;
    private ClusterMessageHandler clusterMessageHandler;


    public ClusterNodeClient(ClusterNodeConf conf) {
        this.conf = conf;
        this.clusterMessageHandler = new DefaultClusterMessageHandler();
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(3));
        // channel factory
        bootstrap().channelFactory(TcpChannelProvider.NIO_CONNECTOR);
    }

    @Override
    public JConnection connect(UnresolvedAddress address, boolean async) {
        setOptions();

        final Bootstrap boot = bootstrap();
        final SocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());

        // 重连watchdog
        watchdog = new ConnectionWatchdog(boot, timer, socketAddress, new NettyChannelGroup(address)) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new IdleStateChecker(timer, 0, JConstants.WRITER_IDLE_TIME_SECONDS, 0),
                        idleStateTrigger,
                        new MessageDecoder(),
                        encoder,
                        handler
                };
            }
        };
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
            if (!async) {
                future.sync();
            }
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


    @Override
    public void shutdown() {
        shutdownGracefully();
    }

    @Override
    public void reportNodeInfo() {
        ClusterMessage msg = new ClusterMessage();
        msg.setSerializerCode(SerializerType.PROTO_STUFF.value());
        msg.setMessageCode(JProtocolHeader.CLUSTER_NODE_REGISTER);
        msg.setSourceNodeId(conf.getNodeId());
        msg.setData(conf);
        watchdog.channel().write(msg);
    }

    @Override
    public void updateNodeInfo(int load, Map<String, String> extend) {
        conf.setCurrentLoad(load);
        if (!CollectionUtils.isEmpty(extend)) {
            conf.getExtend().putAll(extend);
        }
    }

    @Override
    public boolean sendMessageToCluster(Object data, Set<String> clusterNodeIds) {
        checkNotNull(data);
        if (CollectionUtils.isEmpty(clusterNodeIds)) {
            return false;
        }

        ClusterMessage msg = new ClusterMessage();
        msg.setTargetNodeIds(clusterNodeIds);
        msg.setSerializerCode(SerializerType.PROTO_STUFF.value());
        msg.setMessageCode(JProtocolHeader.CLUSTER_MESSAGE_TO_CLUSTER);
        msg.setSourceNodeId(conf.getNodeId());
        msg.setData(data);

        JChannel channel = watchdog.channel();
        if (channel == null) {
            return false;
        }

        channel.write(msg);
        return true;
    }

//    @Override
//    public boolean connectToCluster(Object data) {
//        checkNotNull(data);
//
//        ClusterMessage msg = new ClusterMessage();
//        msg.setSerializerCode(SerializerType.PROTO_STUFF.value());
//        msg.setMessageCode(JProtocolHeader.CLUSTER_MESSAGE_TO_CENTER);
//        msg.setSourceNodeId(clusterNodeConf.getNodeId());
//        msg.setData(data);
//
//        JChannel channel = watchdog.channel();
//        if (channel == null) {
//            return false;
//        }
//
//        channel.write(msg);
//        return true;
//    }

    @Override
    public boolean broadcastMessageToCluster(Object data) {
        checkNotNull(data);
        ClusterMessage msg = new ClusterMessage();
        msg.setSerializerCode(SerializerType.PROTO_STUFF.value());
        msg.setMessageCode(JProtocolHeader.CLUSTER_MESSAGE_TO_CLUSTER);
        msg.setData(data);
        msg.setSourceNodeId(conf.getNodeId());

        JChannel channel = watchdog.channel();
        if (channel == null) {
            return false;
        }

        channel.write(msg);
        return true;
    }

    static class MessageDecoder extends ReplayingDecoder<MessageDecoder.State> {

        public MessageDecoder() {
            super(MessageDecoder.State.HEADER_MAGIC);
        }

        // 协议头
        private final JProtocolHeader header = new JProtocolHeader();

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            switch (state()) {
                case HEADER_MAGIC:
                    checkMagic(in.readShort());             // MAGIC
                    checkpoint(MessageDecoder.State.HEADER_SIGN);
                case HEADER_SIGN:
                    header.sign(in.readByte());             // 消息标志位
                    checkpoint(MessageDecoder.State.HEADER_STATUS);
                case HEADER_STATUS:
                    in.readByte();                          // no-op
                    checkpoint(MessageDecoder.State.HEADER_ID);
                case HEADER_ID:
                    header.id(in.readLong());               // 消息id
                    checkpoint(MessageDecoder.State.HEADER_BODY_LENGTH);
                case HEADER_BODY_LENGTH:
                    header.bodyLength(in.readInt());        // 消息体长度
                    checkpoint(MessageDecoder.State.BODY);
                case BODY:
                    byte s_code = header.serializerCode();
                    switch (header.messageCode()) {
                        case JProtocolHeader.CLUSTER_MESSAGE_TO_CLUSTER: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);
                            Serializer serializer = SerializerFactory.getSerializer(s_code);
                            ClusterMessage clusterMessage = serializer.readObject(bytes, ClusterMessage.class);
                            out.add(clusterMessage);
                            break;
                        }
                        default:
                            throw IoSignals.ILLEGAL_SIGN;

                    }
                    checkpoint(MessageDecoder.State.HEADER_MAGIC);
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

    @ChannelHandler.Sharable
    static class MessageEncoder extends MessageToByteEncoder<ClusterMessage> {

        @Override
        protected void encode(ChannelHandlerContext ctx, ClusterMessage msg, ByteBuf out) throws Exception {
            byte s_code = msg.getSerializerCode();
            byte sign = (byte) ((s_code << 4) + msg.getMessageCode());
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
            try {
                if (msg instanceof ClusterMessage) {
                    ClusterMessage obj = (ClusterMessage) msg;
                    switch (obj.getMessageCode()) {
                        case JProtocolHeader.CLUSTER_MESSAGE_TO_CLUSTER: {
                            clusterMessageHandler.handle(obj.getSourceNodeId(), obj.getData());
                            break;
                        }
                        default: {
                            logger.warn("Unexpected msg header received: {}.", obj.getMessageCode());
                        }
                    }
                } else {
                    logger.warn("Unexpected msg type received: {}.", msg.getClass());
                }
            } finally {
                ReferenceCountUtil.release(msg);
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

    static class ClusterForwardHandlerLoader {
        static final ClusterForwardHandler clusterForwardHandler;

        static {
            clusterForwardHandler = JServiceLoader.loadFirst(ClusterForwardHandler.class);
        }
    }
}

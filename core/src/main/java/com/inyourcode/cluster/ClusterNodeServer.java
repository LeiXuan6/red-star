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

import com.inyourcode.cluster.api.ClusterMessageHandler;
import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.JServiceLoader;
import com.inyourcode.common.util.Signal;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.transport.api.JConfig;
import com.inyourcode.transport.api.JOption;
import com.inyourcode.transport.api.JProtocolHeader;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.exception.IoSignals;
import com.inyourcode.transport.netty.NettyTcpAcceptor;
import com.inyourcode.transport.netty.TcpChannelProvider;
import com.inyourcode.transport.netty.channel.NettyChannel;
import com.inyourcode.transport.netty.handler.IdleStateChecker;
import com.inyourcode.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.List;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;

/**
 * @author JackLei
 */
public class ClusterNodeServer extends NettyTcpAcceptor {
    private static final Logger logger = LoggerFactory.getLogger(ClusterNodeServer.class);
    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private ClusterMessageHandler clusterMessageHandler;

    public ClusterNodeServer(int port, ClusterMessageHandler clusterMessageHandler) {
        super(port);
        this.clusterMessageHandler = clusterMessageHandler;
    }

    public ClusterNodeServer(int port) {
        this(port, ClusterMessageHandlerLoader.clusterMessageHandler);
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
                                handler);
                    }
                });

        setOptions();

        return boot.bind(localAddress);
    }

    /**
     * **************************************************************************************************
     * Protocol
     * ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     * 2   │   1   │    1   │     8     │      4      │
     * ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     * │       │        │           │             │
     * │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     * │       │        │           │             │
     * └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     * <p>
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
                        case JProtocolHeader.HEARTBEAT:
                            break;
                        case JProtocolHeader.CLUSTER_NODE_REGISTER:
                        case JProtocolHeader.CLUSTER_MESSAGE_TO_CENTER:
                        case JProtocolHeader.CLUSTER_MESSAGE_TO_CLUSTER: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Serializer serializer = SerializerFactory.getSerializer(s_code);
                            ClusterMessage msg = serializer.readObject(bytes, ClusterMessage.class);
                            msg.setSerializerCode(s_code);
                            msg.setMessageCode(header.messageCode());
                            out.add(msg);
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

    /**
     * **************************************************************************************************
     * Protocol
     * ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     * 2   │   1   │    1   │     8     │      4      │
     * ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     * │       │        │           │             │
     * │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     * │       │        │           │             │
     * └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     * <p>
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
     * + 1 // 空
     * + 8 // 消息 id, long 类型
     * + 4 // 消息体 body 长度, int类型
     */
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

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel channel = ctx.channel();

            if (msg instanceof ClusterMessage) {
                ClusterMessage obj = (ClusterMessage) msg;

                switch (obj.getMessageCode()) {
                    case JProtocolHeader.CLUSTER_NODE_REGISTER: {
                        ClusterService.registerClusterNode(ctx.channel(), (ClusterNodeConf) obj.getData());
                        break;
                    }
                    case JProtocolHeader.CLUSTER_MESSAGE_TO_CLUSTER: {
                        ClusterService.forwardClusterMessage(ctx.channel(), obj);
                        break;
                    }
                    case JProtocolHeader.CLUSTER_MESSAGE_TO_CENTER: {
                        clusterMessageHandler.handle(obj.getSourceNodeId(), obj.getData());
                        break;
                    }
                }
            } else {
                logger.warn("Unexpected msg type received:{}.", msg.getClass());
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ctx.fireChannelInactive();
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

    static class ClusterMessageHandlerLoader {
        private static final ClusterMessageHandler clusterMessageHandler;

        static {
            clusterMessageHandler = JServiceLoader.loadFirst(ClusterMessageHandler.class);
        }
    }
}

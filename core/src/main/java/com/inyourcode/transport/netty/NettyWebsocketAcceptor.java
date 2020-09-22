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
package com.inyourcode.transport.netty;

import com.inyourcode.common.util.JConstants;
import com.inyourcode.common.util.StackTraceUtil;
import com.inyourcode.transport.api.JConfig;
import com.inyourcode.transport.api.JConfigGroup;
import com.inyourcode.transport.api.JOption;
import com.inyourcode.transport.api.JProtocolHeader;
import com.inyourcode.transport.api.payload.JResponseBytes;
import com.inyourcode.transport.netty.handler.ProtocolDecoder;
import com.inyourcode.transport.netty.handler.acceptor.AcceptorHandler;
import com.inyourcode.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * @author JackLei
 */
public class NettyWebsocketAcceptor extends NettyAcceptor {
    private static final Logger logger = LoggerFactory.getLogger(NettyTcpAcceptor.class);
    private final boolean nativeEt; // Use native epoll ET
    private final NettyConfig.NettyTcpConfigGroup configGroup = new NettyConfig.NettyTcpConfigGroup();
    private String webSocketPath;

    public NettyWebsocketAcceptor(int port, String webSocketPath) {
        super(Protocol.WEBSOCKET, new InetSocketAddress(port));
        nativeEt = true;
        this.webSocketPath = webSocketPath;
        init();
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 32768);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        ServerBootstrap boot = bootstrap();

        if (isNativeEt()) {
            boot.channelFactory(TcpChannelProvider.NATIVE_ACCEPTOR);
        } else {
            boot.channelFactory(TcpChannelProvider.NIO_ACCEPTOR);
        }

        SelfSignedCertificate ssc = null;
        SslContext sslContext = null;
        try {
            ssc = new SelfSignedCertificate();
            sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } catch (CertificateException e) {
            logger.error("bind exception", StackTraceUtil.stackTrace(e));
        } catch (SSLException e) {
            logger.error("bind exception", StackTraceUtil.stackTrace(e));
        }

        WebsocketChannelInitializer channelInitializer = new WebsocketChannelInitializer(sslContext, webSocketPath, timer);
        boot.childHandler(channelInitializer);
        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        ServerBootstrap boot = bootstrap();

        // parent options
        NettyConfig.NettyTcpConfigGroup.ParentConfig parent = configGroup.parent();
        boot.option(ChannelOption.SO_BACKLOG, parent.getBacklog());
        boot.option(ChannelOption.SO_REUSEADDR, parent.isReuseAddress());
        if (parent.getRcvBuf() > 0) {
            boot.option(ChannelOption.SO_RCVBUF, parent.getRcvBuf());
        }

        // child options
        NettyConfig.NettyTcpConfigGroup.ChildConfig child = configGroup.child();
        boot.childOption(ChannelOption.SO_REUSEADDR, child.isReuseAddress())
                .childOption(ChannelOption.SO_KEEPALIVE, child.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, child.isTcpNoDelay())
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, child.isAllowHalfClosure());
        if (child.getRcvBuf() > 0) {
            boot.childOption(ChannelOption.SO_RCVBUF, child.getRcvBuf());
        }
        if (child.getSndBuf() > 0) {
            boot.childOption(ChannelOption.SO_SNDBUF, child.getSndBuf());
        }
        if (child.getLinger() > 0) {
            boot.childOption(ChannelOption.SO_LINGER, child.getLinger());
        }
        if (child.getIpTos() > 0) {
            boot.childOption(ChannelOption.IP_TOS, child.getIpTos());
        }
        int bufLowWaterMark = child.getWriteBufferLowWaterMark();
        int bufHighWaterMark = child.getWriteBufferHighWaterMark();
        if (bufLowWaterMark >= 0 && bufHighWaterMark > 0) {
            WriteBufferWaterMark waterMark = new WriteBufferWaterMark(bufLowWaterMark, bufHighWaterMark);
            boot.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
        }
    }

    @Override
    public JConfigGroup configGroup() {
        return configGroup;
    }

    @Override
    public void start() throws InterruptedException {
        start(true);
    }

    @Override
    public void start(boolean sync) throws InterruptedException {
        // wait until the server socket is bind succeed.
        ChannelFuture future = bind(localAddress).sync();

        logger.info("Jupiter TCP server start" + (sync ? ", and waits until the server socket closed." : ".")
                + JConstants.NEWLINE + " {}.", toString());

        if (sync) {
            // wait until the server socket is closed.
            future.channel().closeFuture().sync();
        }
    }

    @Override
    public void setIoRatio(int bossIoRatio, int workerIoRatio) {
        EventLoopGroup boss = boss();
        if (boss instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) boss).setIoRatio(bossIoRatio);
        } else if (boss instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) boss).setIoRatio(bossIoRatio);
        }

        EventLoopGroup worker = worker();
        if (worker instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) worker).setIoRatio(workerIoRatio);
        } else if (worker instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) worker).setIoRatio(workerIoRatio);
        }
    }

    @Override
    protected EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory) {
        return isNativeEt() ? new EpollEventLoopGroup(nThreads, tFactory) : new NioEventLoopGroup(nThreads, tFactory);
    }

    /**
     * Netty provides the native socket transport for Linux using JNI based on Epoll Edge Triggered(ET).
     */
    public boolean isNativeEt() {
        return nativeEt && NativeSupport.isSupportNativeET();
    }

    @Override
    public String toString() {
        return "Socket address:[" + localAddress + ']' + ", nativeET: " + isNativeEt()
                + JConstants.NEWLINE + bootstrap();
    }

    static class WebsocketChannelInitializer extends ChannelInitializer {
        SslContext sslContext;
        String webSocketPath;
        HashedWheelTimer timer;
        AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
        AcceptorHandler handler;

        public WebsocketChannelInitializer(SslContext sslContext, String webSocketPath, HashedWheelTimer timer) {
            this.sslContext = sslContext;
            this.webSocketPath = webSocketPath;
            this.timer = timer;
            handler = new AcceptorHandler();
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            //ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
//            ch.pipeline().addLast(new IdleStateChecker(timer, JConstants.READER_IDLE_TIME_SECONDS, 0, 0));
//            ch.pipeline().addLast(idleStateTrigger);
            ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            ch.pipeline().addLast(new HttpServerCodec());
            ch.pipeline().addLast(new HttpObjectAggregator(65536));
            ch.pipeline().addLast(new WebSocketServerCompressionHandler());
            ch.pipeline().addLast(new WebSocketServerProtocolHandler(webSocketPath, null, true));
            ch.pipeline().addLast(new BinaryFrameDecoder());
            ch.pipeline().addLast(new ProtocolDecoder());
            ch.pipeline().addLast(new BinaryFrameEncoder());
            ch.pipeline().addLast(handler);
        }
    }

    static class BinaryFrameDecoder extends MessageToMessageDecoder<WebSocketFrame> {

        @Override
        protected void decode(ChannelHandlerContext channelHandlerContext, WebSocketFrame webSocketFrame, List<Object> list) throws Exception {
            ByteBuf content = webSocketFrame.content();
            list.add(content);
            content.retain();
        }
    }

    static class BinaryFrameEncoder extends MessageToMessageEncoder<JResponseBytes> {

        @Override
        protected void encode(ChannelHandlerContext ctx, JResponseBytes response, List<Object> list) throws Exception {
            ByteBuf out = ctx.alloc().buffer();
            byte s_code = response.serializerCode();
            byte sign = (byte) ((s_code << 4) + JProtocolHeader.RESPONSE);
            byte status = response.status();
            long invokeId = response.id();
            byte[] bytes = response.bytes();

            out.writeShort(JProtocolHeader.MAGIC)
                    .writeByte(sign)
                    .writeByte(status)
                    .writeLong(invokeId)
                    .writeInt(bytes.length)
                    .writeBytes(bytes);

            BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(out);
            list.add(binaryWebSocketFrame);

        }
    }
}

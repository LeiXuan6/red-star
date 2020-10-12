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
package com.inyourcode.transport.netty.websocket;

import com.inyourcode.common.util.JConstants;
import com.inyourcode.transport.api.JConfig;
import com.inyourcode.transport.api.JConfigGroup;
import com.inyourcode.transport.api.JOption;
import com.inyourcode.transport.api.processor.ProviderProcessor;
import com.inyourcode.transport.netty.NativeSupport;
import com.inyourcode.transport.netty.NettyAcceptor;
import com.inyourcode.transport.netty.NettyConfig;
import com.inyourcode.transport.netty.NettyTcpAcceptor;
import com.inyourcode.transport.netty.TcpChannelProvider;
import com.inyourcode.transport.netty.handler.acceptor.AcceptorHandler;
import com.inyourcode.transport.session.BasicSessionFactory;
import com.inyourcode.transport.session.BasicSessionProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

/**
 * @author JackLei
 */
public class NettyWebsocketAcceptor extends NettyAcceptor {

    private static final Logger logger = LoggerFactory.getLogger(NettyTcpAcceptor.class);
    private final boolean nativeEt; // Use native epoll ET
    private final NettyConfig.NettyTcpConfigGroup configGroup = new NettyConfig.NettyTcpConfigGroup();
    private AcceptorHandler acceptorHandler = new AcceptorHandler();
    private ProviderProcessor providerProcessor;

    public NettyWebsocketAcceptor(int port, ProviderProcessor providerProcessor) {
        super(Protocol.HTTP, new InetSocketAddress(port));
        nativeEt = true;
        this.providerProcessor = providerProcessor;
        init();
    }

    public NettyWebsocketAcceptor(int port){
        this(port,new BasicSessionProcessor(new BasicSessionFactory()));
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
        acceptorHandler.processor(this.providerProcessor);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        ServerBootstrap boot = bootstrap();

        if (isNativeEt()) {
            boot.channelFactory(TcpChannelProvider.NATIVE_ACCEPTOR);
        } else {
            boot.channelFactory(TcpChannelProvider.NIO_ACCEPTOR);
        }

        WebSocketServerInitializer channelInitializer = new WebSocketServerInitializer(null, acceptorHandler);
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

        logger.info("websocket server start" + (sync ? ", and waits until the server socket closed." : ".")
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

}
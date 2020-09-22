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

package com.inyourcode.transport.netty;

import com.inyourcode.common.util.JConstants;
import com.inyourcode.transport.api.JConfig;
import com.inyourcode.transport.api.JOption;
import com.inyourcode.transport.api.processor.ProviderProcessor;
import com.inyourcode.transport.netty.handler.IdleStateChecker;
import com.inyourcode.transport.netty.handler.ProtocolDecoder;
import com.inyourcode.transport.netty.handler.ProtocolEncoder;
import com.inyourcode.transport.netty.handler.acceptor.AcceptorHandler;
import com.inyourcode.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import java.net.SocketAddress;

import static com.inyourcode.common.util.Preconditions.checkNotNull;


/**
 * Jupiter tcp acceptor based on netty.
 *
 * *********************************************************************
 *            I/O Request                       I/O Response
 *                 │                                 △
 *                                                   │
 *                 │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┼ ─ ─ ─ ─ ─ ─ ─ ─
 * │               │                                                  │
 *                                                   │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
 *     IdleStateChecker#inBound          IdleStateChecker#outBound
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘   │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐                                     │
 *     AcceptorIdleStateTrigger                      │
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                                     │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
 *          ProtocolDecoder                   ProtocolEncoder
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘   │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐                                     │
 *          AcceptorHandler                          │
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                                     │
 *                 │                                 │
 * │                    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐                     │
 *                 ▽                                 │
 * │               ─ ─ ▷│       Processor       ├ ─ ─▷                │
 *
 * │                    └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                     │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 *
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class JNettyTcpAcceptor extends NettyTcpAcceptor {

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final ProtocolEncoder encoder = new ProtocolEncoder();
    private final AcceptorHandler handler = new AcceptorHandler();

    public JNettyTcpAcceptor(int port) {
        super(port);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress) {
        super(localAddress);
    }

    public JNettyTcpAcceptor(int port, int nWorks) {
        super(port, nWorks);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress, int nWorks) {
        super(localAddress, nWorks);
    }

    public JNettyTcpAcceptor(int port, boolean nativeEt) {
        super(port, nativeEt);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress, boolean nativeEt) {
        super(localAddress, nativeEt);
    }

    public JNettyTcpAcceptor(int port, int nWorks, boolean nativeEt) {
        super(port, nWorks, nativeEt);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress, int nWorks, boolean nativeEt) {
        super(localAddress, nWorks, nativeEt);
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
        boot.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new IdleStateChecker(timer, JConstants.READER_IDLE_TIME_SECONDS, 0, 0),
                        idleStateTrigger,
                        new ProtocolDecoder(),
                        encoder,
                        handler);
            }
        });

        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    public void withProcessor(ProviderProcessor processor) {
        handler.processor(checkNotNull(processor, "processor"));
    }
}

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
import com.inyourcode.common.util.NamedThreadFactory;
import com.inyourcode.transport.api.JAcceptor;
import com.inyourcode.transport.api.JConfig;
import com.inyourcode.transport.api.JOption;
import com.inyourcode.transport.api.processor.ProviderProcessor;
import com.inyourcode.transport.netty.estimator.JMessageSizeEstimator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyAcceptor implements JAcceptor {

    protected final Protocol protocol;
    protected final SocketAddress localAddress;

    protected final HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("acceptor.timer"));

    private ServerBootstrap bootstrap;
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private int nWorkers;

    protected volatile ByteBufAllocator allocator;

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress) {
        this(protocol, localAddress, JConstants.AVAILABLE_PROCESSORS << 1);
    }

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress, int nWorkers) {
        this.protocol = protocol;
        this.localAddress = localAddress;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory bossFactory = new DefaultThreadFactory("jupiter.acceptor.boss", Thread.MAX_PRIORITY);
        ThreadFactory workerFactory = new DefaultThreadFactory("jupiter.acceptor.worker", Thread.MAX_PRIORITY);
        boss = initEventLoopGroup(1, bossFactory);
        worker = initEventLoopGroup(nWorkers, workerFactory);

        bootstrap = new ServerBootstrap().group(boss, worker);

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.IO_RATIO, 100);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.IO_RATIO, 100);
        child.setOption(JOption.PREFER_DIRECT, true);
        child.setOption(JOption.USE_POOLED_ALLOCATOR, true);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public SocketAddress localAddress() {
        return localAddress;
    }

    @Override
    public int boundPort() {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new UnsupportedOperationException("Unsupported address type to get port");
        }
        return ((InetSocketAddress) localAddress).getPort();
    }

    @Override
    public void withProcessor(ProviderProcessor processor) {
        // the default implementation does nothing
    }

    @Override
    public void shutdownGracefully() {
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }

    protected void setOptions() {
        JConfig parent = configGroup().parent(); // parent options
        JConfig child = configGroup().child(); // child options

        setIoRatio(parent.getOption(JOption.IO_RATIO), child.getOption(JOption.IO_RATIO));

        boolean direct = child.getOption(JOption.PREFER_DIRECT);
        if (child.getOption(JOption.USE_POOLED_ALLOCATOR)) {
            if (direct) {
                allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new PooledByteBufAllocator(false);
            }
        } else {
            if (direct) {
                allocator = new UnpooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new UnpooledByteBufAllocator(false);
            }
        }
        bootstrap.childOption(ChannelOption.ALLOCATOR, allocator)
                .childOption(ChannelOption.MESSAGE_SIZE_ESTIMATOR, JMessageSizeEstimator.DEFAULT);
    }

    /**
     * Which allows easy bootstrap of {@link io.netty.channel.ServerChannel}.
     */
    protected ServerBootstrap bootstrap() {
        return bootstrap;
    }

    /**
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-creates
     * {@link io.netty.channel.Channel}.
     */
    protected EventLoopGroup boss() {
        return boss;
    }

    /**
     * The {@link EventLoopGroup} for the child. These {@link EventLoopGroup}'s are used to
     * handle all the events and IO for {@link io.netty.channel.Channel}'s.
     */
    protected EventLoopGroup worker() {
        return worker;
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.
     * The default value is {@code 50}, which means the event loop will try to spend the same
     * amount of time for I/O as for non-I/O tasks.
     */
    public abstract void setIoRatio(int bossIoRatio, int workerIoRatio);

    /**
     * Create a new {@link io.netty.channel.Channel} and bind it.
     */
    protected abstract ChannelFuture bind(SocketAddress localAddress);

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory}.
     */
    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);
}

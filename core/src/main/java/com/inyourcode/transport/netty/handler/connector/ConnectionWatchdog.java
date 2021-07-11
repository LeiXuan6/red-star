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

package com.inyourcode.transport.netty.handler.connector;

import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.channel.JChannelGroup;
import com.inyourcode.transport.netty.channel.NettyChannel;
import com.inyourcode.transport.netty.handler.ChannelHandlerHolder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Connections watchdog.
 *
 * jupiter
 * org.jupiter.transport.netty.handler.connector
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionWatchdog.class);

    private static final int ST_STARTED = 1;
    private static final int ST_STOPPED = 2;

    private final Bootstrap bootstrap;
    private final Timer timer;
    private final SocketAddress remoteAddress;
    private final JChannelGroup group;

    private volatile int state;
    private int attempts;
    private boolean autoReconnect = true;

    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, SocketAddress remoteAddress, JChannelGroup group) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.remoteAddress = remoteAddress;
        this.group = group;

        start();
    }

    public boolean isStarted() {
        return state == ST_STARTED;
    }

    public void start() {
        state = ST_STARTED;
    }

    public void forbidAutoConnect() {
        this.autoReconnect = false;
    }

    public void stop() {
        state = ST_STOPPED;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (group != null) {
            group.add(NettyChannel.attachChannel(channel));
        }

        attempts = 0;

        logger.info("Connects with {}.", channel);

        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        boolean doReconnect = isStarted() && (group == null || (group.size() < group.getCapacity()));
        if (doReconnect && autoReconnect) {
            if (attempts < 12) {
                attempts++;
            }
            long timeout = 2 << attempts;
            timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
        }

        logger.warn("Disconnects with {}, address: {}, reconnect: {}.", ctx.channel(), remoteAddress, doReconnect);

        ctx.fireChannelInactive();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (group != null && group.size() >= group.getCapacity()) {
            logger.warn("Cancel reconnecting with {}.", remoteAddress);
            return;
        }

        ChannelFuture future;
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(remoteAddress);
        }

        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                boolean succeed = f.isSuccess();

                logger.warn("Reconnects with {}, {}.", remoteAddress, succeed ? "succeed" : "failed");

                if (!succeed) {
                    f.channel().pipeline().fireChannelInactive();
                }
            }
        });
    }

    public JChannel channel(){
        if (group == null){
            return null;
        }
        if (group.isEmpty()) {
            return null;
        }
        return group.next();
    }
}

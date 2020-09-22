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

package com.inyourcode.transport.netty.handler.acceptor;

import com.inyourcode.common.util.Signal;
import com.inyourcode.transport.api.Status;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.exception.IoSignals;
import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.api.processor.ProviderProcessor;
import com.inyourcode.transport.netty.channel.NettyChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.atomic.AtomicInteger;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;


/**
 * jupiter
 * org.jupiter.transport.netty.handler.acceptor
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class AcceptorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AcceptorHandler.class);

    private static final AtomicInteger channelCounter = new AtomicInteger(0);

    private ProviderProcessor processor;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof JRequestBytes) {
            JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
            try {
                processor.handleRequest(jChannel, (JRequestBytes) msg);
            } catch (Throwable t) {
                processor.handleException(jChannel, (JRequestBytes) msg, Status.SERVER_ERROR, t);
            }
        } else {
            logger.warn("Unexpected msg type received:{}.", msg.getClass());

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.incrementAndGet();

        logger.info("Connects with {} as the {}th channel.", ctx.channel(), count);
        processor.channelActive(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.getAndDecrement();

        logger.warn("Disconnects with {} as the {}th channel.", ctx.channel(), count);
        processor.channelInactive(ctx);
        super.channelInactive(ctx);
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

    public ProviderProcessor processor() {
        return processor;
    }

    public void processor(ProviderProcessor processor) {
        this.processor = processor;
    }
}

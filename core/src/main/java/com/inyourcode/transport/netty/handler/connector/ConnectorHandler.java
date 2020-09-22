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

import com.inyourcode.common.util.Signal;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.exception.IoSignals;
import com.inyourcode.transport.api.payload.JResponseBytes;
import com.inyourcode.transport.api.processor.ConsumerProcessor;
import com.inyourcode.transport.netty.channel.NettyChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.transport.netty.handler.connector
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class ConnectorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorHandler.class);

    private ConsumerProcessor processor;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof JResponseBytes) {
            JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
            try {
                processor.handleResponse(jChannel, (JResponseBytes) msg);
            } catch (Throwable t) {
                logger.error("An exception has been caught {}, on {} #channelRead().", stackTrace(t), jChannel);
            }
        } else {
            logger.warn("Unexpected message type received: {}.", msg.getClass());

            ReferenceCountUtil.release(msg);
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

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        processor.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        processor.channelInactive(ctx);
    }

    public ConsumerProcessor processor() {
        return processor;
    }

    public void processor(ConsumerProcessor processor) {
        this.processor = processor;
    }
}

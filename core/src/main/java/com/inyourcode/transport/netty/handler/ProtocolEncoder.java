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

package com.inyourcode.transport.netty.handler;

import com.inyourcode.transport.api.JProtocolHeader;
import com.inyourcode.transport.api.payload.BytesHolder;
import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.api.payload.JResponseBytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

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
 * + 1 // 消息标志位, 低地址4位用来表示消息类型Request/Response/Heartbeat等, 高地址4位用来表示序列化类型
 * + 1 // 状态位, 设置请求响应状态
 * + 8 // 消息 id, long 类型
 * + 4 // 消息体 body 长度, int类型
 *
 * jupiter
 * org.jupiter.transport.netty.handler
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class ProtocolEncoder extends MessageToByteEncoder<BytesHolder> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BytesHolder msg, ByteBuf out) throws Exception {
        if (msg instanceof JRequestBytes) {
            doEncodeRequest((JRequestBytes) msg, out);
        } else if (msg instanceof JResponseBytes) {
            doEncodeResponse((JResponseBytes) msg, out);
        } else {
            throw new IllegalArgumentException(msg.getClass().getName());
        }
    }

    private void doEncodeRequest(JRequestBytes request, ByteBuf out) {
        byte s_code = request.serializerCode();
        byte sign = (byte) ((s_code << 4) + JProtocolHeader.REQUEST);
        long invokeId = request.invokeId();
        byte[] bytes = request.bytes();
        int length = bytes.length;

        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(sign)
                .writeByte(0x00)
                .writeLong(invokeId)
                .writeInt(length)
                .writeBytes(bytes);
    }

    private void doEncodeResponse(JResponseBytes response, ByteBuf out) {
        byte s_code = response.serializerCode();
        byte sign = (byte) ((s_code << 4) + JProtocolHeader.RESPONSE);
        byte status = response.status();
        long invokeId = response.id();
        byte[] bytes = response.bytes();
        int length = bytes.length;

        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(sign)
                .writeByte(status)
                .writeLong(invokeId)
                .writeInt(length)
                .writeBytes(bytes);
    }
}

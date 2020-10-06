/*
 * Copyright (c) 2020The red-star Project
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
package com.inyourcode.transport.netty.websocket.codec;

import com.inyourcode.transport.api.JProtocolHeader;
import com.inyourcode.transport.api.Status;
import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.api.payload.JResponseBytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * @author JackLei
 */
public class WSBinaryFrameEncoder  extends MessageToMessageEncoder<com.inyourcode.transport.api.payload.BytesHolder> {

    @Override
    protected void encode(ChannelHandlerContext ctx, com.inyourcode.transport.api.payload.BytesHolder byteHolder, List<Object> list) throws Exception {
        ByteBuf out = ctx.alloc().buffer();
        if (byteHolder instanceof JResponseBytes) {
            byte s_code = byteHolder.serializerCode();
            byte sign = (byte) ((s_code << 4) + JProtocolHeader.RESPONSE);
            byte status = ((JResponseBytes)byteHolder).status();
            long invokeId = ((JResponseBytes)byteHolder).id();
            byte[] bytes = byteHolder.bytes();

            out.writeShort(JProtocolHeader.MAGIC)
                    .writeByte(sign)
                    .writeByte(status)
                    .writeLong(invokeId)
                    .writeInt(bytes.length)
                    .writeBytes(bytes);
        }else if( byteHolder instanceof JRequestBytes){
            byte serializerCode = byteHolder.serializerCode();
            byte sign = JProtocolHeader.toSign(serializerCode, JProtocolHeader.REQUEST);
            long invokeId = ((JRequestBytes)byteHolder).invokeId();
            byte[] bytes = byteHolder.bytes();

            out.writeShort(JProtocolHeader.MAGIC)
                    .writeByte(sign)
                    .writeByte(Status.OK.value())
                    .writeLong(invokeId)
                    .writeInt(bytes.length)
                    .writeBytes(bytes);
        } else {
            throw new UnsupportedOperationException("unsupport msg encod, msg = " + byteHolder);
        }

        BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(out);
        list.add(binaryWebSocketFrame);
    }
}


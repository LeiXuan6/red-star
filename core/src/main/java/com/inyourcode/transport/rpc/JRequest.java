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

package com.inyourcode.transport.rpc;

import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.rpc.metadata.MessageWrapper;

/**
 * Consumer's request data.
 *
 * 请求信息载体.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JRequest {

    private final JRequestBytes requestBytes;   // 请求bytes
    private MessageWrapper message;             // 请求对象

    public JRequest() {
        this(new JRequestBytes());
    }

    public JRequest(JRequestBytes requestBytes) {
        this.requestBytes = requestBytes;
    }

    public JRequestBytes requestBytes() {
        return requestBytes;
    }

    public long invokeId() {
        return requestBytes.invokeId();
    }

    public long timestamp() {
        return requestBytes.timestamp();
    }

    public byte serializerCode() {
        return requestBytes.serializerCode();
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        requestBytes.bytes(serializerCode, bytes);
    }

    public MessageWrapper message() {
        return message;
    }

    public void message(MessageWrapper message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "JRequest{" +
                "invokeId=" + invokeId() +
                ", timestamp=" + timestamp() +
                ", serializerCode=" + serializerCode() +
                ", message=" + message +
                '}';
    }
}

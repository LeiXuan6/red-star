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

package com.inyourcode.transport.rpc.consumer.processor.task;


import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.payload.JResponseBytes;
import com.inyourcode.transport.rpc.JResponse;
import com.inyourcode.transport.rpc.consumer.future.DefaultInvokeFuture;
import com.inyourcode.transport.rpc.metadata.ResultWrapper;

/**
 * jupiter
 * consumer.processor.task
 *
 * @author jiachun.fjc
 */
public class MessageTask implements Runnable {

    private final JChannel channel;
    private final JResponse response;

    public MessageTask(JChannel channel, JResponse response) {
        this.channel = channel;
        this.response = response;
    }

    @Override
    public void run() {
        // stack copy
        final JResponse _response = response;
        final JResponseBytes _responseBytes = _response.responseBytes();

        byte s_code = _response.serializerCode();
        byte[] bytes = _responseBytes.bytes();
        _responseBytes.nullBytes();

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        _response.result(serializer.readObject(bytes, ResultWrapper.class));

        DefaultInvokeFuture.received(channel, _response);
    }
}

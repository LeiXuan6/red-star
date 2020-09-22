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

package com.inyourcode.transport.rpc.provider.processor;

import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.transport.api.Status;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.channel.JFutureListener;
import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.api.payload.JResponseBytes;
import com.inyourcode.transport.api.processor.ProviderProcessor;
import com.inyourcode.transport.rpc.JRequest;
import com.inyourcode.transport.rpc.control.FlowController;
import com.inyourcode.transport.rpc.metadata.ResultWrapper;
import com.inyourcode.transport.rpc.provider.LookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * provider.processor
 *
 * @author jiachun.fjc
 */
public abstract class AbstractProviderProcessor implements
        ProviderProcessor, LookupService, FlowController<JRequest> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProviderProcessor.class);

    @Override
    public void handleException(JChannel channel, JRequestBytes request, Status status, Throwable cause) {
        handleException(channel, request.invokeId(), request.serializerCode(), status.value(), cause);
    }

    public void handleException(JChannel channel, JRequest request, Status status, Throwable cause) {
        handleException(channel, request.invokeId(), request.serializerCode(), status.value(), cause);
    }

    private void handleException(JChannel channel, long invokeId, byte s_code, byte status, Throwable cause) {
        logger.error("An exception has been caught while processing request: {}, {}.", invokeId, stackTrace(cause));

        ResultWrapper result = new ResultWrapper();
        result.setError(cause);

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        byte[] bytes = serializer.writeObject(result);

        JResponseBytes response = new JResponseBytes(invokeId);
        response.status(status);
        response.bytes(s_code, bytes);

        channel.write(response, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                logger.debug("Service error message sent out: {}.", channel);
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                logger.warn("Service error message sent failed: {}, {}.", channel, stackTrace(cause));
            }
        });
    }
}

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

package com.inyourcode.transport.rpc.consumer.processor;

import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.payload.JResponseBytes;
import com.inyourcode.transport.api.processor.ConsumerProcessor;
import com.inyourcode.transport.rpc.JResponse;
import com.inyourcode.transport.rpc.consumer.processor.task.MessageTask;

import java.util.concurrent.Executor;

/**
 * The default implementation of consumer's processor.
 *
 * jupiter
 * consumer.processor
 *
 * @author jiachun.fjc
 */
public class DefaultConsumerProcessor implements ConsumerProcessor {

    private final Executor executor;

    public DefaultConsumerProcessor() {
        this(ConsumerExecutors.executor());
    }

    public DefaultConsumerProcessor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void handleResponse(JChannel channel, JResponseBytes responseBytes) throws Exception {
        MessageTask task = new MessageTask(channel, new JResponse(responseBytes));
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }
}

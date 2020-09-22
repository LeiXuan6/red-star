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
import com.inyourcode.transport.api.Directory;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.rpc.JRequest;
import com.inyourcode.transport.rpc.JServer;
import com.inyourcode.transport.rpc.control.ControlResult;
import com.inyourcode.transport.rpc.control.FlowController;
import com.inyourcode.transport.rpc.metadata.ServiceWrapper;
import com.inyourcode.transport.rpc.provider.processor.task.MessageTask;

import java.util.concurrent.Executor;

/**
 * jupiter
 * provider.processor
 *
 * @author jiachun.fjc
 */
public class DefaultProviderProcessor extends AbstractProviderProcessor {

    private final JServer server;
    private final Executor executor;

    public DefaultProviderProcessor(JServer server) {
        this(server, ProviderExecutors.executor());
    }

    public DefaultProviderProcessor(JServer server, Executor executor) {
        this.server = server;
        this.executor = executor;
    }

    @Override
    public void handleRequest(JChannel channel, JRequestBytes requestBytes) throws Exception {
        MessageTask task = new MessageTask(this, channel, new JRequest(requestBytes));
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return server.lookupService(directory);
    }

    @Override
    public ControlResult flowControl(JRequest request) {
        // 全局流量控制
        FlowController<JRequest> controller = server.globalFlowController();
        if (controller == null) {
            return ControlResult.ALLOWED;
        }
        return controller.flowControl(request);
    }
}

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

package com.inyourcode.transport.rpc.consumer.cluster;

import com.inyourcode.transport.rpc.JClient;
import com.inyourcode.transport.rpc.consumer.dispatcher.Dispatcher;

/**
 * jupiter
 * consumer.cluster
 *
 * @author jiachun.fjc
 */
public abstract class AbstractClusterInvoker implements ClusterInvoker {

    protected final JClient client;
    protected final Dispatcher dispatcher;

    public AbstractClusterInvoker(JClient client, Dispatcher dispatcher) {
        this.client = client;
        this.dispatcher = dispatcher;
    }

    @Override
    public String toString() {
        return name();
    }
}

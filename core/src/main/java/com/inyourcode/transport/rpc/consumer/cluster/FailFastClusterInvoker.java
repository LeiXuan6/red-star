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
import com.inyourcode.transport.rpc.consumer.future.InvokeFuture;

/**
 * 快速失败, 只发起一次调用, 失败立即报错(jupiter缺省设置)
 *
 * 通常用于非幂等性的写操作.
 *
 * https://en.wikipedia.org/wiki/Fail-fast
 *
 * jupiter
 * consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailFastClusterInvoker extends AbstractClusterInvoker {

    public FailFastClusterInvoker(JClient client, Dispatcher dispatcher) {
        super(client, dispatcher);
    }

    @Override
    public String name() {
        return "Fail-fast";
    }

    @Override
    public <T> InvokeFuture<T> invoke(String methodName, Object[] args, Class<T> returnType) throws Exception {
        return dispatcher.dispatch(client, methodName, args, returnType);
    }
}

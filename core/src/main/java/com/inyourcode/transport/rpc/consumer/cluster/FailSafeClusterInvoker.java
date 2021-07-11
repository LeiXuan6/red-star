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
import com.inyourcode.transport.rpc.consumer.future.FailSafeInvokeFuture;
import com.inyourcode.transport.rpc.consumer.future.InvokeFuture;

/**
 * 失败安全, 出现异常时, 直接忽略.
 *
 * 通常用于写入审计日志等操作.
 *
 * http://en.wikipedia.org/wiki/Fail-safe
 *
 * jupiter
 * consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailSafeClusterInvoker extends AbstractClusterInvoker {

    public FailSafeClusterInvoker(JClient client, Dispatcher dispatcher) {
        super(client, dispatcher);
    }

    @Override
    public String name() {
        return "Fail-safe";
    }

    @Override
    public <T> InvokeFuture<T> invoke(String methodName, Object[] args, Class<T> returnType) throws Exception {
        InvokeFuture<T> future = dispatcher.dispatch(client, methodName, args, returnType);
        return FailSafeInvokeFuture.with(future);
    }
}

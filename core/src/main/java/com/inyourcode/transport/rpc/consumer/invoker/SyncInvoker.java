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

package com.inyourcode.transport.rpc.consumer.invoker;

import com.inyourcode.transport.rpc.consumer.cluster.ClusterInvoker;
import com.inyourcode.transport.rpc.consumer.future.InvokeFuture;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;

/**
 * Synchronous call.
 *
 * 同步调用.
 *
 * jupiter
 * consumer.invoker
 *
 * @author jiachun.fjc
 */
public class SyncInvoker {

    private final ClusterInvoker clusterInvoker;

    public SyncInvoker(ClusterInvoker clusterInvoker) {
        this.clusterInvoker = clusterInvoker;
    }

    @RuntimeType
    public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
        InvokeFuture<?> future = clusterInvoker.invoke(method.getName(), args, method.getReturnType());
        return future.getResult();
    }
}

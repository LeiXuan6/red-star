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

package com.inyourcode.transport.rpc.consumer.dispatcher;

import com.inyourcode.transport.rpc.ConsumerHook;
import com.inyourcode.transport.rpc.JClient;
import com.inyourcode.transport.rpc.consumer.future.InvokeFuture;
import com.inyourcode.transport.rpc.metadata.ServiceMetadata;

import java.util.List;
import java.util.Map;

/**
 * jupiter
 * consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public interface Dispatcher {

    <T> InvokeFuture<T> dispatch(JClient client, String methodName, Object[] args, Class<T> returnType);

    ServiceMetadata metadata();

    Dispatcher hooks(List<ConsumerHook> hooks);

    Dispatcher timeoutMillis(long timeoutMillis);

    Dispatcher methodsSpecialTimeoutMillis(Map<String, Long> methodsSpecialTimeoutMillis);
}

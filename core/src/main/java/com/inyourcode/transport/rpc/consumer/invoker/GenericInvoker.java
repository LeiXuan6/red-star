/*
 * Copyright (c) 2016 The Jupiter Project
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

/**
 * 简单的理解, 泛化调用就是不依赖二方包, 通过传入方法名, 方法参数值, 就可以调用服务.
 *
 * jupiter
 * consumer.invoker
 *
 * @author jiachun.fjc
 */
public interface GenericInvoker {

    Object $invoke(String methodName, Object... args) throws Throwable;
}

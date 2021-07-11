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

package com.inyourcode.transport.rpc;

import com.inyourcode.transport.api.channel.JChannel;

/**
 * Consumer's hook.
 *
 * 客户端的钩子函数.
 *
 * 在请求发送时触发 {@link #before(JRequest, JChannel)} 方法;
 * 在响应回来时触发 {@link #after(JResponse, JChannel)} 方法.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface ConsumerHook {

    ConsumerHook[] EMPTY_HOOKS = new ConsumerHook[0];

    /**
     * Triggered when the request data sent to the network.
     */
    void before(JRequest request, JChannel channel);

    /**
     * Triggered when the server returns the result.
     */
    void after(JResponse response, JChannel channel);
}

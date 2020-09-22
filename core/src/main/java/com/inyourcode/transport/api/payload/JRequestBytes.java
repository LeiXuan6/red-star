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

package com.inyourcode.transport.api.payload;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求的消息体bytes载体, 避免在IO线程中序列化/反序列化, jupiter-transport这一层不关注消息体的对象结构.
 *
 * jupiter
 * payload
 *
 * @author jiachun.fjc
 */
public class JRequestBytes extends BytesHolder {

    // 请求ID自增器, 用于映射 <ID, Request, Response> 三元组,
    // 收到当前ID对应的Response并处理完成后这个ID就可以复用了, 所以不用担心AtomicLong溢出
    private static final AtomicLong invokeIdGenerator = new AtomicLong(0);

    // 用于映射 <ID, Request, Response> 三元组
    private final long invokeId;
    // jupiter-transport层会在协议解析完成后打上一个时间戳, 用于后续监控对该请求的处理时间
    private transient long timestamp;

    public JRequestBytes() {
        this(invokeIdGenerator.getAndIncrement());
    }

    public JRequestBytes(long invokeId) {
        this.invokeId = invokeId;
    }

    public long invokeId() {
        return invokeId;
    }

    public long timestamp() {
        return timestamp;
    }

    public void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

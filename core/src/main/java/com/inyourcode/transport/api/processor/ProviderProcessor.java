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

package com.inyourcode.transport.api.processor;


import com.inyourcode.transport.api.Status;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.payload.JRequestBytes;
import io.netty.channel.ChannelHandlerContext;

/**
 * Provider's processor.
 *
 * jupiter
 * processor
 *
 * @author jiachun.fjc
 */
public interface ProviderProcessor {

    /**
     * 处理正常请求
     */
    void handleRequest(JChannel channel, JRequestBytes request) throws Exception;

    /**
     * 处理异常
     */
    void handleException(JChannel channel, JRequestBytes request, Status status, Throwable cause);


    default void channelActive(ChannelHandlerContext ctx) throws Exception {

    }

    default void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }
}

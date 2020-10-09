/*
 * Copyright (c) 2020 The red-star Project
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
package com.inyourcode.transport.session.api;


import io.netty.channel.Channel;

/**
 *
 * @author JackLei
 */
public interface Session<M> {
     /** session的唯一id  */
     long getId();

     /** channel */
     Channel channel();

     /** 是否可用 */
     boolean isActive();

     /** 获取channel中的属性 */
    <T> T getAttribute(Class key);

    /** 设置channel属性 */
    void setAttribute(Class key,Object val);

    /** 移除属性 */
    void removeAttribute(Class key);

    /** 写消息到channel */
    void write(M message);

    /** 写消息到channel ,并刷新到缓冲区 */
    void writeAndFlush(M message);

    /** 刷新缓冲区 */
    void flush();

    /** 是否验证通过 */
    default boolean auth(){
        return true;
    }

    /** 验证的密钥 */
    default String authKey(){
        return "AUTH";
    }
}

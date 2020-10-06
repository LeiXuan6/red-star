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

import com.inyourcode.transport.netty.channel.NettyChannel;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 *
 * @author JackLei
 */
public interface ISessionFactory {
    AttributeKey<Session> ATTRIBUTE_SESSION_KEY = AttributeKey.valueOf("netty-channel-session");

    Session create(Channel channel);

    void remove(Channel nettyChannel);

    Session get(NettyChannel channel);
}

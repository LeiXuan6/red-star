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
package com.inyourcode.transport.session;

import com.inyourcode.transport.netty.channel.NettyChannel;
import com.inyourcode.transport.session.api.ISessionFactory;
import com.inyourcode.transport.session.api.Session;
import io.netty.channel.Channel;
import io.netty.util.Attribute;

/**
 *
 * @author JackLei
 */
public class BasicSessionFactory implements ISessionFactory {

    @Override
    public Session create(Channel channel) {
        Attribute<Session> attr = channel.attr(ATTRIBUTE_SESSION_KEY);
        Session session = attr.get();
        if(session == null){
            session = new BasicSession(channel);
            Session ifAbsent = attr.setIfAbsent(session);
            if(ifAbsent != null){
                session = ifAbsent;
            }
        }
        return session;
    }

    @Override
    public void remove(Channel channel) {
        Attribute<Session> attr = channel.attr(ATTRIBUTE_SESSION_KEY);
        attr.set(null);
    }

    @Override
    public Session get(NettyChannel nettyChannel) {
        Attribute<Session> attr = nettyChannel.channel().attr(ATTRIBUTE_SESSION_KEY);
        return  attr.get();
    }
}

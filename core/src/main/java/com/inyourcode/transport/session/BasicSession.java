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
import com.inyourcode.transport.session.api.Session;
import io.netty.channel.Channel;
import org.omg.CORBA.ByteHolder;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * session 基础实现
 * @author JackLei
 */
public class BasicSession implements Session<ByteHolder> {
    private long id;
    private Channel channel;
    private ConcurrentHashMap<Object,Object> attributeMap = new ConcurrentHashMap<>();

    public BasicSession(long id, Channel nettyChannel) {
        this.id = id;
        this.channel = nettyChannel;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void setAttribute(Object key, Object val) {
        attributeMap.put(key,val);
    }

    @Override
    public void removeAttribute(Object key) {
        attributeMap.remove(key);
    }

    @Override
    public void write(ByteHolder message) {
        channel.write(message);
    }

    @Override
    public void writeAndFlush(ByteHolder message) {
        channel.writeAndFlush(message);
    }

    @Override
    public void flush() {
        channel.flush();
    }

    @Override
    public Object getAttribute(Object key) {
        return attributeMap.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicSession that = (BasicSession) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

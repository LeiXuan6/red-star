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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * session 基础实现
 * @author JackLei
 */
public class BasicSession implements Session {
    private long id;
    private Channel channel;
    private ConcurrentHashMap<Class,Object> attributeMap = new ConcurrentHashMap<>();

    public BasicSession() {
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setId(long id) {
        this.id = id;
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
    public void setAttribute(Class key, Object val) {
        attributeMap.put(key,val);
    }

    @Override
    public void removeAttribute(Class key) {
        attributeMap.remove(key);
    }

    @Override
    public void write(Object message) {
        channel.write(message);
    }

    @Override
    public void writeAndFlush(Object message) {
        channel.writeAndFlush(message);
    }

    @Override
    public void flush() {
        channel.flush();
    }

    @Override
    public <T> T getAttribute(Class<T> key) {
        return (T) attributeMap.get(key);
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

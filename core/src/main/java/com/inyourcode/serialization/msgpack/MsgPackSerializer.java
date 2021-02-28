/*
 * Copyright (c) 2020The red-star Project
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
package com.inyourcode.serialization.msgpack;

import com.inyourcode.common.util.StackTraceUtil;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerType;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author JackLei
 */
public class MsgPackSerializer extends Serializer {
    private static final Logger logger = LoggerFactory.getLogger(MsgPackSerializer.class);

    @Override
    public byte code() {
        return SerializerType.MSGPACK.value();
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        try {
            MessagePack messagePack = new MessagePack();
            return messagePack.write(obj);
        } catch (IOException e) {
            logger.error("write obj error", StackTraceUtil.stackTrace(e));
        }
        return null;
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        MessagePack messagePack = new MessagePack();
        try {
            return messagePack.read(bytes, clazz);
        } catch (IOException e) {
            logger.error("read obj error, bytes={}",StackTraceUtil.stackTrace(e));
        }
        return null;
    }
}

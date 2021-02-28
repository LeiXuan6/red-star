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

package com.inyourcode.serialization.protostuff;

import com.inyourcode.common.util.Reflects;
import com.inyourcode.common.util.SystemPropertyUtil;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerType;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Protostuff的序列化/反序列化实现, jupiter中默认的实现.
 *
 * jupiter
 * org.jupiter.serialization.proto
 *
 * @author jiachun.fjc
 */
public class ProtoStuffSerializer extends Serializer {
    private static final Logger logger = LoggerFactory.getLogger(ProtoStuffSerializer.class);


    /**
     * If true, the constructor will always be obtained from {@code ReflectionFactory.newConstructorFromSerialization}.
     *
     * Enable this if you intend to avoid deserialize objects whose no-args constructor initializes (unwanted)
     * internal state. This applies to complex/framework objects.
     *
     * If you intend to fill default field values using your default constructor, leave this disabled. This normally
     * applies to java beans/data objects.
     */
    public static final boolean ALWAYS_USE_SUN_REFLECTION_FACTORY = true;

    static {
        // 禁止反序列化时构造方法被调用, 防止有些类的构造方法内有令人惊喜的逻辑
        // 详见 io.protostuff.runtime.RuntimeEnv
        String value = String.valueOf(ALWAYS_USE_SUN_REFLECTION_FACTORY);
        SystemPropertyUtil
                .setProperty("protostuff.runtime.always_use_sun_reflection_factory", value);
    }

    private static final ConcurrentMap<Class<?>, Schema<?>> schemaCache = new ConcurrentHashMap<>();

    private static final ThreadLocal<LinkedBuffer> bufThreadLocal = new ThreadLocal<LinkedBuffer>() {

        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(DEFAULT_BUF_SIZE);
        }
    };

    @Override
    public byte code() {
        return SerializerType.PROTO_STUFF.value();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> byte[] writeObject(T obj) {
        Schema<T> schema = getSchema((Class<T>) obj.getClass());

        LinkedBuffer buf = bufThreadLocal.get();
        try {
            // TODO toByteArray里面一坨的 memory copy 如果哪天有好办法了可以优化一下
            return ProtostuffIOUtil.toByteArray(obj, schema, buf);
        } finally {
            buf.clear(); // for reuse
        }
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        T msg = Reflects.newInstance(clazz, true);
        Schema<T> schema = getSchema(clazz);

        ProtostuffIOUtil.mergeFrom(bytes, offset, length, msg, schema);
        return msg;
    }

    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) schemaCache.get(clazz);
        if (schema == null) {
            Schema<T> newSchema = RuntimeSchema.createFrom(clazz);
            schema = (Schema<T>) schemaCache.putIfAbsent(clazz, newSchema);
            if (schema == null) {
                schema = newSchema;
            }
        }
        return schema;
    }

    @Override
    public String toString() {
        return "proto_stuff:(code=" + code() + ")";
    }
}

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
package com.inyourcode.example.redis.hash;

import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.serialization.api.SerializerType;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽象R
 * @author JackLei
 */
public abstract class RedisHashModel {
    private static Serializer serializer = SerializerFactory.getSerializer(SerializerType.PROTO_STUFF.value());
    protected Map<String, Object> dataMap = new HashMap<>();

    public <T> T  hget(Class<T> clazz){
        return (T) dataMap.get(RedisHashType.getHashKey(clazz));
    }

    public void hset( Object object){
        dataMap.put(RedisHashType.getHashKey(object.getClass()), object);
    }

    public abstract String key();

    public final Map<String, byte[]> toBytes() {
        Map<String, byte[]> bytesMap = new HashMap<>();
        for(Map.Entry<String, Object> entry: dataMap.entrySet()){
            Object document = entry.getValue();
            String hashKey = entry.getKey();
            byte[] bytes = serializer.writeObject(document);
            bytesMap.put(hashKey, bytes);
        }
        return bytesMap;
    }

    public final void fromBytes(Map<String, byte[]> bytesMap) {
        for (Map.Entry<String, byte[]> entry : bytesMap.entrySet()) {
            String hashKey = entry.getKey();
            byte[] bytes = entry.getValue();
            Class modelClass = RedisHashType.getHashClass(hashKey);
            Object o = serializer.readObject(bytes, modelClass);
            dataMap.put(hashKey, o);
        }
    }
}

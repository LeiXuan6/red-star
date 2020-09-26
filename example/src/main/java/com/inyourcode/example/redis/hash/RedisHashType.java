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

/**
 * @author JackLei
 */
public enum  RedisHashType {
    ITEM("item", ItemDocument.class),
    PLAYER("player", PlayerDocument.class)


    ;

    private String hashKey;
    private Class clazz;

    RedisHashType(String hashKey, Class clazz) {
        this.hashKey = hashKey;
        this.clazz = clazz;
    }

    public static String getHashKey(Class clazz){
        for(RedisHashType hashType : values()){
            if (hashType.clazz == clazz){
                return hashType.hashKey;
            }
        }
        throw new RuntimeException("can not found hash key, clazz=" + clazz);
    }

    public static Class getHashClass(String hashKey){
        for(RedisHashType hashType : values()){
            if (hashType.hashKey.equals(hashKey)){
                return hashType.clazz;
            }
        }
        throw new RuntimeException("can not found hash clazz, hash key=" + hashKey);
    }
}

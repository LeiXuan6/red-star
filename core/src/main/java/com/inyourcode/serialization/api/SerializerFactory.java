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

package com.inyourcode.serialization.api;

import com.inyourcode.common.util.JServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds all serializers.
 *
 * jupiter
 * org.jupiter.serialization
 *
 * @author jiachun.fjc
 */
public final class SerializerFactory {

    private static final Logger logger = LoggerFactory.getLogger(SerializerFactory.class);

    private static final Map<Byte,Serializer> serializerMapping = new HashMap<>();

    static {
        List<Serializer> serializerList = JServiceLoader.loadAll(Serializer.class);

        logger.info("Support serializers: {}.", serializerList);

        for (Serializer s : serializerList) {
            serializerMapping.put(s.code(), s);
        }
    }

    public static Serializer getSerializer(byte code) {
        Serializer serializer = serializerMapping.get(code);

        if (serializer == null) {
            throw new NullPointerException("unsupported serializerImpl with code: " + code);
        }

        return serializer;
    }
}

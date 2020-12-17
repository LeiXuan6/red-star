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
package com.inyourcode.excel.serializer;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author JackLei
 */
public class JavaEnumSerializer implements ObjectDeserializer {
    private static  final Logger LOGGER = LoggerFactory.getLogger(JavaEnumSerializer.class);

    @Override
    public <T> T deserialze(DefaultJSONParser defaultJSONParser, Type type, Object o) {
        try {
            String content = defaultJSONParser.parse().toString();
            Integer enumType = Integer.valueOf(content);
            Method[] declaredMethods = ((Class) type).getDeclaredMethods();
            Method method = null;
            for (Method tempMethod : declaredMethods) {
                if (tempMethod.getName().contains("getEnumType")) {
                    method = tempMethod;
                    break;
                }
            }
            return (T) method.invoke(type, enumType);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Failed to serialize enum data,class = {}", type, e);
        }
        return null;
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}

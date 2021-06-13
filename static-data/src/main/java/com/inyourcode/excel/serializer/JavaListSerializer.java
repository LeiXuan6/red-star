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

import com.alibaba.excel.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Type;

/**
 *
 * @author JackLei
 */
public class JavaListSerializer implements ObjectDeserializer {

    @Override
    public JavaExcelList deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JavaExcelList javaList = new JavaExcelList();
        String content = parser.parse().toString();
        if (StringUtils.isEmpty(content)) {
            return javaList;
        }
        Type[] actualTypeArguments = ((ParameterizedTypeImpl) type).getActualTypeArguments();

        JSONArray jsonArray = JSONArray.parseArray(content);
        for (int index = 0; index < jsonArray.size(); index++) {

            if (actualTypeArguments[0] == Integer.class) {
                javaList.add(jsonArray.getObject(index, Integer.class));
            } else if (actualTypeArguments[0] == Float.class) {
                javaList.add(jsonArray.getObject(index, Float.class));
            } else {
                javaList.add(jsonArray.getObject(index, String.class));
            }
        }
        return javaList;
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}

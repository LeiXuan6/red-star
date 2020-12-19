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
package com.inyourcode.example.staticdata;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.io.Files;
import com.inyourcode.excel.api.ExcelTable;
import com.inyourcode.excel.api.JavaExcelModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JackLei
 */
public class StaticDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticDataManager.class);
    private Map<Class<? extends JavaExcelModel>, StaticDataContainer> DATA_MAP = new HashMap<>();

    public void load(ApplicationContext applicationContext) {
        Map<String, String> jsonDataMap = new HashMap<>();
        ClassPathResource classPathResource = new ClassPathResource("server-conf");
        try {
            File file = classPathResource.getFile();
            File[] files = file.listFiles();
            for (File jsonFile : files) {
                List<String> strings = Files.readLines(jsonFile, Charset.forName("UTF-8"));
                String jsonData = strings.get(0);
                String fileName = jsonFile.getName();
                jsonDataMap.put(fileName, jsonData);
                LOGGER.info("The Json data read success,file:{},data:{}", fileName, jsonData);
            }

            Map<String, JavaExcelModel> beansOfType = applicationContext.getBeansOfType(JavaExcelModel.class);
            beansOfType.forEach((k, v) -> {
                Class<? extends JavaExcelModel> excelModelClazz = v.getClass();
                ExcelTable annotation = excelModelClazz.getAnnotation(ExcelTable.class);
                String dataFileName = annotation.data();
                String data = jsonDataMap.get(dataFileName);
                Map<Integer, ? extends JavaExcelModel> dataMap = parseToMap(data, Integer.class, excelModelClazz);
                cache(excelModelClazz, dataMap);
            });
        } catch (IOException e) {
            LOGGER.error("Failed to load json data", e);
        }
    }

    public void cache(Class<? extends JavaExcelModel> clazz, Map<Integer, ? extends JavaExcelModel> dataMap) {
        DATA_MAP.put(clazz, StaticDataContainer.valueOf(clazz, dataMap));
    }

    public <T> T getModel(Class<T> clazz, Integer id) {
        StaticDataContainer container = DATA_MAP.get(clazz);
        if (container == null) {
            return null;
        }
        return (T) container.dataMap.get(id);
    }

    private static <K, V> Map<K, V> parseToMap(String json,
                                               Class<K> keyType,
                                               Class<V> valueType) {
        return JSON.parseObject(json, new TypeReference<Map<K, V>>(keyType, valueType) {
        });
    }

    static class StaticDataContainer<T extends JavaExcelModel> {
        Class<? extends JavaExcelModel> clazz;
        Map<Integer, T> dataMap;

        static StaticDataContainer valueOf(Class<? extends JavaExcelModel> clazz, Map<Integer, ? extends JavaExcelModel> dataMap) {
            StaticDataContainer container = new StaticDataContainer();
            container.clazz = clazz;
            container.dataMap = dataMap;
            return container;
        }
    }
}

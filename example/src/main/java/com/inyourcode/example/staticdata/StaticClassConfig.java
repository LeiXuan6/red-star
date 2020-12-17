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

import com.alibaba.fastjson.JSONObject;
import com.google.common.io.Files;
import com.inyourcode.excel.api.ExcelTable;
import com.inyourcode.excel.api.JavaExcelModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JackLei
 */
@Configurable
@Configuration
@PropertySource("classpath:static-data.properties")
@EnableScheduling
@ComponentScan(basePackages = "${app.scanpackages}")
public class StaticClassConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticClassConfig.class);
    @Autowired
    ApplicationContext applicationContext;

    @PostConstruct
    void init() throws IOException {

        Map<String,String> jsonDataMap = new HashMap<>();
        ClassPathResource classPathResource = new ClassPathResource("server-conf");
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
            List<? extends JavaExcelModel> javaExcelModels = JSONObject.parseArray(data, excelModelClazz);
            System.out.println(javaExcelModels);
        });
    }
}

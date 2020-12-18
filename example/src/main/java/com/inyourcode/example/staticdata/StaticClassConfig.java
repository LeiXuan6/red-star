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

import com.inyourcode.example.staticdata.domain.Gift;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.io.IOException;

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
    @Autowired
    StaticDataManager staticDataManager;

    @Bean
    StaticDataManager staticDataBean(){
        return new StaticDataManager();
    }

    @PostConstruct
    void init() throws IOException {
        staticDataManager.load(applicationContext);
        Gift model = staticDataManager.getModel(Gift.class, 3003);
        System.out.println(model);
    }

}

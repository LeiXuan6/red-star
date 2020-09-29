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
package com.inyourcode.example.redis.script;

import com.inyourcode.common.util.Strings;
import com.inyourcode.db.redis.RedisConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Arrays;

/**
 * @author JackLei
 */
public class TestRedisScript {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(RedisConfig.class);
        applicationContext.refresh();

        ScriptSource scriptSource = new ResourceScriptSource(new ClassPathResource("scripts/cas.lua"));
        DefaultRedisScript<Boolean> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setScriptSource(scriptSource);
        defaultRedisScript.setResultType(Boolean.class);

        String playerId = "Test:Login:1001";
        StringRedisTemplate redisTemplate=  applicationContext.getBean(RedisConfig.STRING_REDIS_TEMPLATE, StringRedisTemplate.class);
        ValueOperations<String, String> stringStringValueOperations = redisTemplate.opsForValue();
        String isCreated = stringStringValueOperations.get(playerId);
        if (Strings.isNullOrEmpty(isCreated)){
            boolean isCreatedFromCAS = redisTemplate.execute(defaultRedisScript, Arrays.asList(playerId), "", "created");
            if (isCreatedFromCAS) {
                System.out.println("create ing");
            }else{
                System.out.println("create faile");
            }
        }else{
            System.out.println("was created");
        }



    }
}

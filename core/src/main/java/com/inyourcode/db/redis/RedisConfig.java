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
package com.inyourcode.db.redis;

import com.alibaba.fastjson.JSON;
import com.inyourcode.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;

/**
 * @author JackLei
 */
@Configurable
@Configuration
@PropertySource("classpath:redis.properties")
@ComponentScan(basePackages = "${app.scanpackages}")
public class RedisConfig {
    private  static final Charset UTF8 = Charset.forName("utf-8");
    @Value("${redis.factory.type}")
    private String factoryType;
    @Value("${redis.password}")
    private String password;
    @Value("${redis.sentinel.hosts}")
    private String sentinelHosts;
    @Value("${redis.sentinel.masetername}")
    private String sentinelMasterName;
    @Value("${redis.database}")
    private String dataBaseName;
    @Value("${redis.standalone.host}")
    private String standaloneHost;

    @Bean
    public RedisConnectionFactory jedisConnectionFactory() {
        return buildRedisFactory(factoryType);
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        redisTemplate.setKeySerializer(new RedisKeySerializer(dataBaseName + ":"));
        redisTemplate.setValueSerializer(new RedisByteSerializer());
        redisTemplate.setHashKeySerializer(new RedisKeySerializer(""));
        redisTemplate.setHashValueSerializer(new RedisByteSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;

    }

    class RedisByteSerializer implements RedisSerializer {
        @Override
        public byte[] serialize(Object s) throws SerializationException {
            Class clz = s.getClass();
            if (clz == byte[].class) {
                return (byte[]) s;
            } else if (clz == String.class) {
                return ((String) s).getBytes(UTF8);
            } else {
                return JSON.toJSONString(s).getBytes(UTF8);
            }
        }

        @Override
        public byte[] deserialize(byte[] bytes) throws SerializationException {
            return bytes;
        }
    }

    class RedisKeySerializer implements RedisSerializer {
        private String head;

        public RedisKeySerializer(String head) {
            this.head = head;
        }

        @Override
        public byte[] serialize(Object key) throws SerializationException {
            if (key.getClass() == String.class) {
                return (head + key).getBytes(UTF8);
            } else {
                throw new RuntimeException("not support key type " + key.getClass().getName());
            }
        }

        @Override
        public String deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return "";
            }
            return new String(bytes, UTF8);
        }
    }

    protected RedisConnectionFactory buildRedisFactory(String type) {
        RedisFactoryType factoryType = RedisFactoryType.getFactory(type);
        Preconditions.checkArgument(factoryType != null, "The redis factory type[" + type + "] not found");
        switch (factoryType) {
            case SENTIENL:
                RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
                Preconditions.checkArgument(!StringUtils.isEmpty(sentinelHosts), "sentinel host config error.");
                String[] hostArry = sentinelHosts.split(",");
                for (String host : hostArry) {
                    String[] hostInfo = host.split(":");
                    Preconditions.checkArgument(hostInfo.length == 2, "sentinel host config error.");
                    sentinelConfig.sentinel(hostInfo[0], Integer.valueOf(hostInfo[1]));
                }
                sentinelConfig.setPassword(RedisPassword.of(password));
                sentinelConfig.master(sentinelMasterName);
                return new JedisConnectionFactory(sentinelConfig);
            case STANDALONE:
                Preconditions.checkArgument(!StringUtils.isEmpty(standaloneHost) , "standalone host config error.");
                String[] hostArray = standaloneHost.split(":");
                Preconditions.checkArgument(hostArray.length == 2, "standalone host config error.");
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(hostArray[0], Integer.parseInt(hostArray[1]));
                config.setPassword(password);
                return new JedisConnectionFactory(config);
        }
        return null;
    }

    enum RedisFactoryType {
        SENTIENL("sentinel"),
        STANDALONE("standalone"),

        ;
        private String name;

        RedisFactoryType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static RedisFactoryType getFactory(String type) {
            for (RedisFactoryType factoryType : values()) {
                if (factoryType.name.equals(type)) {
                    return factoryType;
                }
            }
            return null;
        }
    }
}

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

import com.inyourcode.db.redis.RedisConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * @author JackLei
 */
public class TestRedisHash {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(RedisConfig.class);
        applicationContext.refresh();

        long playerId = 1002;
        PlayerCollection playerCollection = new PlayerCollection(playerId);

        ItemDocument itemDocument = new ItemDocument();
        itemDocument.setId(1);
        itemDocument.setNum(1);

        playerCollection.hset(itemDocument);

        PlayerDocument playerDocument  = new PlayerDocument();
        playerDocument.setPlayerId(playerId);
        playerDocument.setName("jacklei");

        playerCollection.hset(playerDocument);

        RedisTemplate redisTemplate = applicationContext.getBean(RedisTemplate.class);
        redisTemplate.opsForHash().putAll(playerCollection.key(), playerCollection.toBytes());

        PlayerCollection deserializeCollection = new PlayerCollection(1002);
        Map entries = redisTemplate.opsForHash().entries(playerCollection.key());
        deserializeCollection.fromBytes(entries);

        System.out.println(deserializeCollection.hget(ItemDocument.class));

    }
}

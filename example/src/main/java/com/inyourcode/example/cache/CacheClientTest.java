package com.inyourcode.example.cache;

import com.inyourcode.cache.client.DefaultCacheClient;
import com.inyourcode.cache.client.JCacheClient;

public class CacheClientTest {
    public static void main(String[] args) throws InterruptedException {
        JCacheClient defaultCacheClient = new DefaultCacheClient();
        defaultCacheClient.connectToCacheServer("127.0.0.1:8888");

        defaultCacheClient.set(1,1);
        Object o = defaultCacheClient.get(1);

        System.out.println("get 1 :" + o);
    }
}

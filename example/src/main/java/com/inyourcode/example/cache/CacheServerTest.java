package com.inyourcode.example.cache;

import com.inyourcode.cache.server.DefaultCacheServer;

public class CacheServerTest {

    public static void main(String[] args) {
        DefaultCacheServer cacheServer = new DefaultCacheServer(8888);
        cacheServer.start();
    }

}

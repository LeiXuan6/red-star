package com.inyourcode.cache.client;

import java.util.Map;

/**
 * 缓存客户端，面向应用程的抽象
 */
public interface JCacheClient {

    Map getHash(Object key);

    Object getHashValue(Object key, Object hashKey);

    void putHashAll(Object key, Map hashMap);

    void delHashValue(Object key, Object hashKey);

    void set(Object key, Object val);

    Object get(Object key);

    void connectToCacheServer(String connectString);
}

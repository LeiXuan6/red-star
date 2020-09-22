package com.inyourcode.cache.provider;

import com.inyourcode.transport.rpc.ServiceProvider;

import java.util.Map;

@ServiceProvider(group = "cache")
public interface RemoteCacheProvider {

    Map getHash(Object key);

    Object getHashValue(Object key, Object hashKey);

    void putHashAll(Object key, Map hashMap);

    void delHashValue(Object key, Object hashKey);

    void set(Object key, Object val);

    Object get(Object key);

}

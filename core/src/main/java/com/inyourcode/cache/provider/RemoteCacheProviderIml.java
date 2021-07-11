package com.inyourcode.cache.provider;

import com.inyourcode.cache.CacheService;
import com.inyourcode.transport.rpc.ServiceProviderImpl;
import java.util.Map;

@ServiceProviderImpl(version = "1.0.0")
public class RemoteCacheProviderIml implements RemoteCacheProvider {

    @Override
    public Map getHash(Object key) {
        return CacheService.getAllHashValue(key);
    }

    @Override
    public Object getHashValue(Object key, Object hashKey) {
        return CacheService.getHashValue(key, hashKey);
    }

    @Override
    public void putHashAll(Object key, Map hashMap) {
        CacheService.hashAll(key, hashMap);
    }

    @Override
    public void delHashValue(Object key, Object hashKey) {
        CacheService.deleteHash(key, hashKey);
    }

    @Override
    public void set(Object key, Object val) {
        CacheService.put(key, val);
    }

    @Override
    public Object get(Object key) {
        return CacheService.get(key);
    }
}

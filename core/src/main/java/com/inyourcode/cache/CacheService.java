package com.inyourcode.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);
    private static final ConcurrentHashMap BASE_CACHE_MAP = new ConcurrentHashMap();
    private static final HashBasedMap HASH_TABLE = new HashBasedMap();
    private static final ReentrantReadWriteLock HASH_TABLE_LOCK = new ReentrantReadWriteLock();

    public static void put(Object key, Object val) {
        BASE_CACHE_MAP.put(key, val);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cache data added successfully, key:{}, val:{}", key, val);
        }
    }

    public static Object get(Object key) {
        return BASE_CACHE_MAP.get(key);
    }

    public static void delelte(Object key) {
        BASE_CACHE_MAP.remove(key);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("cache data delete successfully, key:{}, val:{}", key);
        }
    }

    public static void hash(Object key, Object hashKey, Object val) {
        HASH_TABLE_LOCK.writeLock().lock();
        try {
            HASH_TABLE.hash(key, hashKey, val);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("hash data added successfully, key:{}, hashKey:{}, val:{}", key, hashKey, val);
            }
        } finally {
            HASH_TABLE_LOCK.writeLock().lock();
        }
    }

    public static void hashAll(Object key, Map map) {
        HASH_TABLE_LOCK.writeLock().lock();
        try {
             HASH_TABLE.hashAll(key, map);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("hash map data added successfully, key:{},", key);
            }
        } finally {
            HASH_TABLE_LOCK.writeLock().lock();
        }
    }

    public static Map getAllHashValue(Object key) {
        HASH_TABLE_LOCK.readLock().lock();
        try {
            return HASH_TABLE.getHashMap(key);
        } finally {
            HASH_TABLE_LOCK.readLock().unlock();
        }
    }

    public static Object getHashValue(Object key, Object hashKey) {
        HASH_TABLE_LOCK.readLock().lock();
        try {
            return HASH_TABLE.getHashValue(key, hashKey);
        } finally {
            HASH_TABLE_LOCK.readLock().unlock();
        }
    }

    public static void deleteHash(Object key) {
        HASH_TABLE_LOCK.writeLock().lock();
        try {
            HASH_TABLE.delete(key);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("hash data delete successfully, key:{}", key);
            }
        } finally {
            HASH_TABLE_LOCK.writeLock().lock();
        }
    }

    public static void deleteHash(Object key, Object hashKey) {
        HASH_TABLE_LOCK.writeLock().lock();
        try {
             HASH_TABLE.delete(key, hashKey);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("hash data delete successfully, key:{}, hashKey:{}", key, hashKey);
            }
        } finally {
            HASH_TABLE_LOCK.writeLock().lock();
        }
    }

    static class HashBasedMap {
        ConcurrentHashMap<Object, HashBasedTable> hashbasedMap = new ConcurrentHashMap<>();

        void hash(Object key, Object hashKey, Object data) {
            HashBasedTable hashBasedTable = getTableWhenNullCreate(key);
            hashBasedTable.table.put(hashKey, data);
        }

        void hashAll(Object key, Map map) {
            HashBasedTable hashBasedTable = getTableWhenNullCreate(key);
            hashBasedTable.table.putAll(map);
        }

        Object getHashValue(Object key, Object hashKey) {
            HashBasedTable hashBasedTable = hashbasedMap.get(key);
            if (hashBasedTable == null) {
                return null;
            }
            return hashBasedTable.table.get(hashKey);
        }

        Map getHashMap(Object key) {
            HashBasedTable hashBasedTable = hashbasedMap.get(key);
            if (hashBasedTable == null) {
                return null;
            }
            return hashBasedTable.table;
        }

        void delete(Object key) {
            hashbasedMap.remove(key);
        }

        void delete(Object key, Object hashKey) {
            HashBasedTable hashBasedTable = hashbasedMap.get(key);
            if (hashBasedTable == null) {
                return;
            }

            hashBasedTable.table.remove(hashKey);
        }

        private HashBasedTable getTableWhenNullCreate(Object key) {
            HashBasedTable hashBasedTable = hashbasedMap.get(key);
            if (hashBasedTable == null) {
                hashBasedTable = new HashBasedTable(key);
                HashBasedTable putIfAbsent = hashbasedMap.putIfAbsent(key, hashBasedTable);
                if (putIfAbsent != null) {
                    hashBasedTable = putIfAbsent;
                }
            }
            return hashBasedTable;
        }
    }

    static class HashBasedTable {
        Object key;
        ConcurrentHashMap table = new ConcurrentHashMap();

        HashBasedTable(Object key) {
            this.key = key;
        }
    }

}

package com.academic.planning.service;

import java.time.Duration;

public interface RedisGateway {
    String get(String key);
    void set(String key, String value, Duration ttl);
    void delete(String key);
    void expire(String key, Duration ttl);
    long increment(String key, Duration ttl);
    boolean acquireLock(String key, String owner, Duration ttl);
    boolean setIfLockOwner(String lockKey, String owner, String key, String value, Duration ttl);
    void releaseLock(String key, String owner);
}

package com.academic.planning.service;

import java.time.Duration;
import java.util.Optional;

public interface RedisCacheService {
    <T> Optional<T> get(String key, Class<T> type);
    void put(String key, Object value, Duration ttl);
}

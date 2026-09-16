package com.academic.planning.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.academic.planning.service.RedisCacheService;
import com.academic.planning.service.RedisGateway;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private final RedisGateway redisGateway;
    private final ObjectMapper objectMapper;

    public RedisCacheServiceImpl(RedisGateway redisGateway, ObjectMapper objectMapper) {
        this.redisGateway = redisGateway;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String value = redisGateway.get(key);
            return value == null ? Optional.empty() : Optional.of(objectMapper.readValue(value, type));
        } catch (Exception exception) {
            log.warn("Redis cache read failed for key {}: {}", key, exception.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redisGateway.set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception exception) {
            log.warn("Redis cache write failed for key {}: {}", key, exception.getMessage());
        }
    }
}

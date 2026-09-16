package com.academic.planning.service;

import com.academic.planning.service.impl.RedisGatewayImpl;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RedisGatewayTest {

    @Test
    void shouldRoundTripAgainstLocalRedisWhenAvailable() {
        RedisGateway gateway = new RedisGatewayImpl("127.0.0.1", 6379, "");
        String key = "academic:test:" + UUID.randomUUID();
        try {
            gateway.set(key, "ok", Duration.ofSeconds(30));
        } catch (RuntimeException unavailable) {
            assumeTrue(false, "Local Redis is not available");
        }
        try {
            assertEquals("ok", gateway.get(key));
            gateway.delete(key);
            assertNull(gateway.get(key));
        } finally {
            gateway.delete(key);
        }
    }
}

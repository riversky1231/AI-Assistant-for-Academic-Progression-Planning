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
        String historyKey = key + ":history";
        try {
            gateway.set(key, "ok", Duration.ofSeconds(30));
        } catch (RuntimeException unavailable) {
            assumeTrue(false, "Local Redis is not available");
        }
        try {
            assertEquals("ok", gateway.get(key));
            gateway.delete(key);
            assertNull(gateway.get(key));
            assertEquals(true, gateway.acquireLock(key, "first", Duration.ofSeconds(30)));
            assertEquals(false, gateway.acquireLock(key, "second", Duration.ofSeconds(30)));
            assertEquals(false, gateway.setIfLockOwner(key, "second", historyKey, "stale", Duration.ofSeconds(30)));
            assertNull(gateway.get(historyKey));
            assertEquals(true, gateway.setIfLockOwner(key, "first", historyKey, "current", Duration.ofSeconds(30)));
            assertEquals("current", gateway.get(historyKey));
            gateway.releaseLock(key, "second");
            assertEquals("first", gateway.get(key));
            gateway.releaseLock(key, "first");
            assertNull(gateway.get(key));
            assertEquals(false, gateway.setIfLockOwner(key, "first", historyKey, "expired", Duration.ofSeconds(30)));
            assertEquals("current", gateway.get(historyKey));
            assertEquals(1, gateway.increment(key, Duration.ofSeconds(30)));
            assertEquals(2, gateway.increment(key, Duration.ofSeconds(30)));
        } finally {
            gateway.delete(key);
            gateway.delete(historyKey);
        }
    }
}

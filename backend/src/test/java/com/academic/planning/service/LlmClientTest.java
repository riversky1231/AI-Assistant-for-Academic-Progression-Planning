package com.academic.planning.service;

import com.academic.planning.common.BusinessException;
import com.academic.planning.config.LlmProperties;
import com.academic.planning.service.impl.CompatibleLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class LlmClientTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void disabledConfigurationFailsClearly() {
        var client = new CompatibleLlmClient(new LlmProperties(false, "", "", "", 30), mapper);
        assertEquals(503, assertThrows(BusinessException.class,
                () -> client.complete(List.of(), mapper.createArrayNode())).getStatus().value());
    }

    @Test void sendsCompatibleRequestAndSanitizesFailures() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        var status = new java.util.concurrent.atomic.AtomicInteger(200);
        server.createContext("/v1/chat/completions", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = (status.get() == 200
                    ? "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\"hello\"}}]}"
                    : "secret upstream detail").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var client = new CompatibleLlmClient(new LlmProperties(true,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/", "test-key", "test-model", 3), mapper);
            assertEquals("hello", client.complete(List.of(), mapper.createArrayNode()).path("content").asText());
            assertEquals("Bearer test-key", auth.get());
            assertEquals("test-model", mapper.readTree(body.get()).path("model").asText());
            status.set(401);
            BusinessException error = assertThrows(BusinessException.class,
                    () -> client.complete(List.of(), mapper.createArrayNode()));
            assertEquals(502, error.getStatus().value());
            assertFalse(error.getMessage().contains("secret"));
        } finally {
            server.stop(0);
        }
    }

    @Test void rejectsTruncatedMalformedAndUnexpectedResponses() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var responseBody = new AtomicReference<String>();
        server.createContext("/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            var client = new CompatibleLlmClient(new LlmProperties(true,
                    "http://127.0.0.1:" + server.getAddress().getPort(), "test-key", "test-model", 3), mapper);
            for (String body : List.of("private upstream text", "null", "{}",
                    "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"role\":\"assistant\",\"content\":\"truncated\"}}]}",
                    "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"system\",\"content\":\"unexpected\"}}]}")) {
                responseBody.set(body);
                BusinessException error = assertThrows(BusinessException.class,
                        () -> client.complete(List.of(), mapper.createArrayNode()));
                assertEquals(502, error.getStatus().value());
                assertFalse(error.getMessage().contains("private upstream"));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test void mapsSlowProviderToGatewayTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                Thread.sleep(1500);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            var client = new CompatibleLlmClient(new LlmProperties(true,
                    "http://127.0.0.1:" + server.getAddress().getPort(), "test-key", "test-model", 1), mapper);
            assertEquals(504, assertThrows(BusinessException.class,
                    () -> client.complete(List.of(), mapper.createArrayNode())).getStatus().value());
        } finally {
            server.stop(0);
        }
    }
}

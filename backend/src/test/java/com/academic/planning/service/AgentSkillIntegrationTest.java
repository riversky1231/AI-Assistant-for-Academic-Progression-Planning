package com.academic.planning.service;

import com.academic.planning.common.GlobalExceptionHandler;
import com.academic.planning.config.LlmProperties;
import com.academic.planning.controller.AgentController;
import com.academic.planning.mapper.PermissionMapper;
import com.academic.planning.security.AuthInterceptor;
import com.academic.planning.service.impl.AgentServiceImpl;
import com.academic.planning.service.impl.CompatibleLlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises MVC -> conversation -> agent -> actual HTTP serialization, without a paid model. */
class AgentSkillIntegrationTest {
    @Test void reportsInjectedSkillEvenWithoutToolCallsAndTracksReferenceReadsOnFollowup() throws Exception {
        var mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var skill = new SkillLoader(mapper);
        String path = "references/research/01-writings.md";
        var requests = new CopyOnWriteArrayList<JsonNode>();
        var count = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requests.add(mapper.readTree(exchange.getRequestBody()));
            boolean readReference = count.incrementAndGet() == 2;
            Object message = readReference
                    ? Map.of("role", "assistant", "tool_calls", List.of(Map.of("id", "reference_1", "type", "function",
                        "function", Map.of("name", "read_skill_resource", "arguments", mapper.writeValueAsString(Map.of("path", path))))))
                    : Map.of("role", "assistant", "content", "先明确目标，再比较家庭承受能力和专业路径。");
            byte[] response = mapper.writeValueAsBytes(Map.of("choices", List.of(Map.of(
                    "finish_reason", readReference ? "tool_calls" : "stop", "message", message))));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var redis = mock(RedisGateway.class);
            var permissions = mock(PermissionMapper.class);
            var stored = new HashMap<String, String>();
            stored.put("academic:session:test", "1");
            when(redis.get(anyString())).thenAnswer(i -> stored.get(i.getArgument(0)));
            when(redis.acquireLock(anyString(), anyString(), any())).thenAnswer(i -> {
                stored.put(i.getArgument(0), i.getArgument(1)); return true;
            });
            when(redis.setIfLockOwner(anyString(), anyString(), anyString(), anyString(), any())).thenAnswer(i -> {
                if (!java.util.Objects.equals(stored.get(i.getArgument(0)), i.getArgument(1))) return false;
                stored.put(i.getArgument(2), i.getArgument(3)); return true;
            });
            doAnswer(i -> { stored.remove(i.getArgument(0)); return null; })
                    .when(redis).releaseLock(anyString(), anyString());
            when(permissions.selectPermissionCodesByUserId(1)).thenReturn(List.of("recommend:use"));
            when(permissions.selectRoleCodesByUserId(1)).thenReturn(List.of());

            var client = new CompatibleLlmClient(new LlmProperties(true,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key", "test-model", 3), mapper);
            var schools = mock(SchoolService.class);
            var recommendations = mock(RecommendationService.class);
            var agent = new AgentServiceImpl(client, schools, recommendations, mapper, validators.getValidator(), skill);
            var conversations = new AgentConversationService(agent, redis, mapper);
            var mvc = MockMvcBuilders.standaloneSetup(new AgentController(conversations))
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                    .addInterceptors(new AuthInterceptor(redis, permissions)).build();

            String first = mvc.perform(post("/agent/chat").header("satoken", "test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsBytes(Map.of("message", "我想学计算机，如何比较成本？"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sources").isEmpty())
                    .andExpect(jsonPath("$.data.skill.name").value("zhangxuefeng-skill"))
                    .andExpect(jsonPath("$.data.skill.instructions_injected").value(true))
                    .andExpect(jsonPath("$.data.skill.instructions_sha256").value(skill.instructionsSha256()))
                    .andExpect(jsonPath("$.data.skill.resources_read").isEmpty())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            String id = mapper.readTree(first).path("data").path("conversation_id").asText();
            assertFalse(id.isBlank());
            assertEquals(1, requests.size());
            assertTrue(requests.get(0).path("messages").get(0).path("content").asText().endsWith(skill.instructions()));

            mvc.perform(post("/agent/chat").header("satoken", "test").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsBytes(Map.of("message", "这个分析框架的著作依据是什么？", "conversationId", id))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.conversation_id").value(id))
                    .andExpect(jsonPath("$.data.skill.instructions_injected").value(true))
                    .andExpect(jsonPath("$.data.skill.resources_read[0]").value(path))
                    .andExpect(jsonPath("$.data.sources[0].tool").value("read_skill_resource"));
            assertEquals(3, requests.size());
            var followup = requests.get(1).path("messages");
            assertTrue(followup.get(0).path("content").asText().endsWith(skill.instructions()));
            assertEquals("我想学计算机，如何比较成本？", followup.get(1).path("content").asText());
            var continuation = requests.get(2).path("messages");
            var toolResult = continuation.get(continuation.size() - 1);
            assertEquals("tool", toolResult.path("role").asText());
            assertEquals(skill.readResource(path).content(), mapper.readTree(toolResult.path("content").asText()).path("content").asText());
            verifyNoInteractions(schools, recommendations);
        } finally {
            server.stop(0);
        }
    }
}

package com.academic.planning.service.impl;

import com.academic.planning.common.BusinessException;
import com.academic.planning.config.LlmProperties;
import com.academic.planning.service.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class CompatibleLlmClient implements LlmClient {
    private final LlmProperties config;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public CompatibleLlmClient(LlmProperties config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    @Override
    public JsonNode complete(List<JsonNode> messages, JsonNode tools) {
        if (!config.enabled() || config.apiKey().isBlank() || config.model().isBlank()
                || config.baseUrl().isBlank() || config.timeoutSeconds() < 1 || config.timeoutSeconds() > 60) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "LLM 服务尚未配置");
        }
        try {
            String body = mapper.writeValueAsString(Map.of("model", config.model(),
                    "messages", messages, "tools", tools, "tool_choice", "auto",
                    "stream", false, "max_tokens", 2048));
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            config.baseUrl().replaceAll("/+$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型服务请求失败，请稍后重试");
            }
            JsonNode choice = mapper.readTree(response.body()).path("choices").path(0);
            JsonNode message = choice.path("message");
            if (!message.isObject() || !"assistant".equals(message.path("role").asText())
                    || !("stop".equals(choice.path("finish_reason").asText())
                    || "tool_calls".equals(choice.path("finish_reason").asText()))) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型返回不完整或无效");
            }
            return message;
        } catch (HttpTimeoutException exception) {
            throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, "模型服务响应超时");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "模型请求已中断");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            // Never expose upstream bodies, credentials, or request content.
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型服务连接失败或响应无效");
        }
    }
}

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

/**
 * OpenAI 兼容协议的 LLM 客户端，使用 JDK 原生 HttpClient 调用 {baseUrl}/chat/completions。
 * 要点：非流式、tool_choice=auto；任何失败只抛业务异常，绝不把上游响应正文或密钥泄露给前端。
 */
@Service
public class CompatibleLlmClient implements LlmClient {
    private final LlmProperties config;
    private final ObjectMapper mapper;
    // 连接超时固定 10 秒；单请求超时由配置 timeoutSeconds 控制
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public CompatibleLlmClient(LlmProperties config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    @Override
    public JsonNode complete(List<JsonNode> messages, JsonNode tools) {
        // 未启用或配置不完整，直接返回 503「尚未配置」，不发起请求
        if (!config.enabled() || config.apiKey().isBlank() || config.model().isBlank()
                || config.baseUrl().isBlank() || config.timeoutSeconds() < 1 || config.timeoutSeconds() > 60) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "LLM 服务尚未配置");
        }
        try {
            // 组装非流式请求体：模型 + 消息 + 工具 + tool_choice=auto + max_tokens=2048
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
            // 严格校验返回结构：必须是 assistant 消息，finish_reason 只能是 stop 或 tool_calls
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
            // 兜底：连接失败等一律 502，不暴露上游响应正文、密钥或请求内容
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型服务连接失败或响应无效");
        }
    }
}

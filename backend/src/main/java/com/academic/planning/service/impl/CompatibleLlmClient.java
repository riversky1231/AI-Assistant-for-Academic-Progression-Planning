package com.academic.planning.service.impl;

import com.academic.planning.common.BusinessException;
import com.academic.planning.config.LlmProperties;
import com.academic.planning.service.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI 兼容协议的 LLM 客户端，使用 JDK 原生 HttpClient 调用 {baseUrl}/chat/completions。
 * 要点：非流式、tool_choice=auto；任何失败只抛业务异常，绝不把上游响应正文或密钥泄露给前端。
 */
@Service
public class CompatibleLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(CompatibleLlmClient.class);
    private static final int MAX_REQUEST_BYTES = 96 * 1024;
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
                || config.baseUrl().isBlank() || config.timeoutSeconds() < 1 || config.timeoutSeconds() > 60
                || config.maxOutputTokens() < 1 || config.maxOutputTokens() > 8192) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "LLM 服务尚未配置");
        }
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startedAt = System.nanoTime();
        try {
            // 组装非流式请求体：模型 + 消息 + 工具 + tool_choice=auto + max_tokens=2048
            String body = mapper.writeValueAsString(Map.of("model", config.model(),
                    "messages", messages, "tools", tools, "tool_choice", tools.isEmpty() ? "none" : "auto",
                    "stream", false, "max_tokens", config.maxOutputTokens()));
            int requestBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (requestBytes > MAX_REQUEST_BYTES) {
                log.warn("LLM request rejected: id={}, model={}, requestBytes={}, maxRequestBytes={}", requestId,
                        config.model(), requestBytes, MAX_REQUEST_BYTES);
                throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "模型上下文过大，请减少查询范围或开启新对话");
            }
            log.info("LLM request: id={}, model={}, messages={}, tools={}, requestBytes={}, maxOutputTokens={}", requestId,
                    config.model(), messages.size(), tools.isArray() ? tools.size() : 0,
                    requestBytes, config.maxOutputTokens());
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            config.baseUrl().replaceAll("/+$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            log.info("LLM response: id={}, model={}, status={}, elapsedMs={}", requestId, config.model(),
                    response.statusCode(), elapsedMs);
            JsonNode payload = mapper.readTree(response.body());
            JsonNode usage = payload.path("usage");
            JsonNode promptDetails = usage.path("prompt_tokens_details");
            log.info("LLM usage: id={}, promptTokens={}, completionTokens={}, totalTokens={}, cachedTokens={}, cacheReadTokens={}, cacheWriteTokens={}", requestId,
                    usage.path("prompt_tokens").asText("unknown"), usage.path("completion_tokens").asText("unknown"),
                    usage.path("total_tokens").asText("unknown"),
                    promptDetails.path("cached_tokens").asText(usage.path("prompt_cache_hit_tokens").asText("unknown")),
                    usage.path("cache_read_input_tokens").asText("unknown"),
                    usage.path("cache_creation_input_tokens").asText("unknown"));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型服务请求失败，请稍后重试");
            }
            JsonNode choice = payload.path("choices").path(0);
            JsonNode message = choice.path("message");
            String finishReason = choice.path("finish_reason").asText("unknown");
            // Only log known protocol values, never arbitrary provider content.
            String safeReason = java.util.Set.of("stop", "tool_calls", "length", "content_filter").contains(finishReason)
                    ? finishReason : "unknown";
            log.info("LLM completion: id={}, finishReason={}", requestId, safeReason);
            if ("length".equals(finishReason)) {
                log.warn("LLM output truncated: id={}, maxOutputTokens={}", requestId, config.maxOutputTokens());
                throw new BusinessException(HttpStatus.BAD_GATEWAY,
                        "模型输出达到长度上限，回答未完成，请缩小问题范围或联系管理员调整输出上限");
            }
            if (!message.isObject() || !"assistant".equals(message.path("role").asText())
                    || !("stop".equals(choice.path("finish_reason").asText())
                    || "tool_calls".equals(choice.path("finish_reason").asText()))) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型返回不完整或无效");
            }
            return message;
        } catch (HttpTimeoutException exception) {
            log.warn("LLM request timed out: id={}, model={}, elapsedMs={}", requestId, config.model(),
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
            throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, "模型服务响应超时");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("LLM request interrupted: id={}, model={}, elapsedMs={}", requestId, config.model(),
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "模型请求已中断");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            // Never expose upstream bodies, credentials, or request content.
            log.warn("LLM request failed: id={}, model={}, elapsedMs={}, error={}", requestId, config.model(),
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis(), exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型服务连接失败或响应无效");
        }
    }
}

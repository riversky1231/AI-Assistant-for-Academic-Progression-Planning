package com.academic.planning.service;

import com.academic.planning.common.BusinessException;
import com.academic.planning.dto.AgentChatRequest;
import com.academic.planning.security.SessionUser;
import com.academic.planning.vo.AgentChatResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/** Redis owns history; a user-scoped lease prevents concurrent writes across instances. */
@Service
public class AgentConversationService {
    private static final Duration TTL = Duration.ofMinutes(30);
    private final AgentService agent;
    private final RedisGateway redis;
    private final ObjectMapper mapper;

    public AgentConversationService(AgentService agent, RedisGateway redis, ObjectMapper mapper) {
        this.agent = agent;
        this.redis = redis;
        this.mapper = mapper;
    }

    public AgentChatResponse chat(AgentChatRequest request, SessionUser user) {
        String id = request.conversationId() == null ? UUID.randomUUID().toString() : request.conversationId();
        String key = "academic:agent:conversation:" + user.userId() + ":" + id;
        // Lease exceeds the bounded four model requests (each configured at most 60 seconds).
        String lock = "academic:agent:busy:" + user.userId();
        String owner = UUID.randomUUID().toString();
        boolean acquired = false;
        try {
            if (redis.increment("academic:agent:rate:" + user.userId(), Duration.ofMinutes(1)) > 10) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "咨询请求过于频繁，请稍后重试");
            }
            acquired = redis.acquireLock(lock, owner, Duration.ofMinutes(10));
            if (!acquired) throw new BusinessException(HttpStatus.CONFLICT, "上一条咨询仍在处理中");
            List<JsonNode> history = new ArrayList<>();
            if (request.conversationId() != null) {
                String saved = redis.get(key);
                if (saved == null) throw new BusinessException(HttpStatus.NOT_FOUND, "会话不存在或已过期，请开始新会话");
                history = mapper.readValue(saved, new TypeReference<ArrayList<JsonNode>>() {});
            }
            AgentChatResponse result = agent.chat(request, user, history);
            history.add(mapper.valueToTree(Map.of("role", "user", "content", request.message())));
            history.add(mapper.valueToTree(Map.of("role", "assistant", "content", result.answer())));
            // Trim complete user/assistant pairs, never a partial tool exchange.
            while (history.size() > 16 || (history.size() > 2 && mapper.writeValueAsString(history).length() > 32000)) {
                history.subList(0, 2).clear();
            }
            // Check ownership and save in one Redis operation: an expired worker cannot overwrite a new turn.
            if (!redis.setIfLockOwner(lock, owner, key, mapper.writeValueAsString(history), TTL)) {
                throw new BusinessException(HttpStatus.CONFLICT, "请求已过期，请重试");
            }
            return new AgentChatResponse(result.answer(), result.sources(), id, result.skill());
        } catch (BusinessException | com.academic.planning.security.AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "会话服务暂不可用");
        } finally {
            if (acquired) {
                try { redis.releaseLock(lock, owner); } catch (RuntimeException ignored) { /* Lease expires automatically. */ }
            }
        }
    }
}

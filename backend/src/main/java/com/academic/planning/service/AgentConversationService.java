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

    /**
     * 会话编排入口：限流 → 加分布式锁 → 读历史 → 调 Agent → 裁剪历史 → 校验锁归属后写回 → 释放锁。
     */
    public AgentChatResponse chat(AgentChatRequest request, SessionUser user) {
        // 无 conversation_id 视为新会话，生成 UUID；否则续接已有会话
        String id = request.conversationId() == null ? UUID.randomUUID().toString() : request.conversationId();
        // 会话历史键：按用户 + 会话隔离
        String key = "academic:agent:conversation:" + user.userId() + ":" + id;
        // Lease exceeds the bounded four model requests (each configured at most 60 seconds).
        // 用户级忙锁键：同一用户同时只允许一条咨询在跑
        String lock = "academic:agent:busy:" + user.userId();
        // owner 是本轮随机标识，用于释放锁/写回时校验「锁仍属于自己」
        String owner = UUID.randomUUID().toString();
        boolean acquired = false;
        try {
            // 限流：每用户每分钟最多 10 次，超出返回 429
            if (redis.increment("academic:agent:rate:" + user.userId(), Duration.ofMinutes(1)) > 10) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "咨询请求过于频繁，请稍后重试");
            }
            // 加锁（SET NX EX，10 分钟）：抢不到说明上一条还在处理，返回 409
            acquired = redis.acquireLock(lock, owner, Duration.ofMinutes(10));
            if (!acquired) throw new BusinessException(HttpStatus.CONFLICT, "上一条咨询仍在处理中");
            List<JsonNode> history = new ArrayList<>();
            // 续聊时从 Redis 读回历史；已过期/不存在返回 404
            if (request.conversationId() != null) {
                String saved = redis.get(key);
                if (saved == null) throw new BusinessException(HttpStatus.NOT_FOUND, "会话不存在或已过期，请开始新会话");
                history = mapper.readValue(saved, new TypeReference<ArrayList<JsonNode>>() {});
            }
            AgentChatResponse result = agent.chat(request, user, history);
            // 把本轮 user/assistant 追加进历史；工具明细不持久化
            history.add(mapper.valueToTree(Map.of("role", "user", "content", request.message())));
            history.add(mapper.valueToTree(Map.of("role", "assistant", "content", result.answer())));
            // Trim complete user/assistant pairs, never a partial tool exchange.
            // 历史裁剪：最多 16 条或 32000 字符，从头部成对删除，绝不截断半次工具交换
            while (history.size() > 16 || (history.size() > 2 && mapper.writeValueAsString(history).length() > 32000)) {
                history.subList(0, 2).clear();
            }
            // Check ownership and save in one Redis operation: an expired worker cannot overwrite a new turn.
            // Lua 原子「校验锁 owner + 写历史」：已过期的 worker 无法覆盖新一轮对话
            if (!redis.setIfLockOwner(lock, owner, key, mapper.writeValueAsString(history), TTL)) {
                throw new BusinessException(HttpStatus.CONFLICT, "请求已过期，请重试");
            }
            return new AgentChatResponse(result.answer(), result.sources(), id, result.skill());
        } catch (BusinessException | com.academic.planning.security.AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "会话服务暂不可用");
        } finally {
            // 释放锁（仅释放属于自己的锁），失败可忽略——锁会自动过期
            if (acquired) {
                try { redis.releaseLock(lock, owner); } catch (RuntimeException ignored) { /* Lease expires automatically. */ }
            }
        }
    }
}

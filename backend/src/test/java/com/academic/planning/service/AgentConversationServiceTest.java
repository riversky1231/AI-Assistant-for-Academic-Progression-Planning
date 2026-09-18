package com.academic.planning.service;

import com.academic.planning.common.BusinessException;
import com.academic.planning.dto.AgentChatRequest;
import com.academic.planning.security.SessionUser;
import com.academic.planning.vo.AgentChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AgentConversationServiceTest {
    private final AgentService agent = mock(AgentService.class);
    private final RedisGateway redis = mock(RedisGateway.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, String> data = new HashMap<>();
    private final SessionUser user = new SessionUser(1, "token", List.of("recommend:use"), List.of());
    private final AgentConversationService service = new AgentConversationService(agent, redis, mapper);

    @BeforeEach void setup() {
        when(redis.get(anyString())).thenAnswer(i -> data.get(i.getArgument(0)));
        when(redis.acquireLock(anyString(), anyString(), any())).thenAnswer(i -> {
            data.put(i.getArgument(0), i.getArgument(1)); return true;
        });
        doAnswer(i -> { data.remove(i.getArgument(0), i.getArgument(1)); return null; }).when(redis).releaseLock(anyString(), anyString());
        when(redis.setIfLockOwner(anyString(), anyString(), anyString(), anyString(), any())).thenAnswer(i -> {
            if (!Objects.equals(data.get(i.getArgument(0)), i.getArgument(1))) return false;
            data.put(i.getArgument(2), i.getArgument(3)); return true;
        });
        when(agent.chat(any(), any(), anyList())).thenReturn(new AgentChatResponse("请提供位次", List.of()));
    }

    @Test void continuesHistoryAndIsolatesUsers() {
        var first = service.chat(new AgentChatRequest("福建物理580分"), user);
        assertNotNull(first.conversationId());
        when(agent.chat(any(), eq(user), anyList())).thenAnswer(i -> {
            List<com.fasterxml.jackson.databind.JsonNode> history = i.getArgument(2);
            assertEquals(2, history.size());
            assertEquals("福建物理580分", history.get(0).path("content").asText());
            return new AgentChatResponse("已推荐", List.of());
        });
        assertEquals(first.conversationId(), service.chat(new AgentChatRequest("15000位", first.conversationId()), user).conversationId());
        var other = new SessionUser(2, "other", user.permissions(), List.of());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.chat(new AgentChatRequest("继续", first.conversationId()), other)).getStatus());
    }

    @Test void failureDoesNotSaveAndReleasesLock() {
        when(agent.chat(any(), any(), anyList())).thenThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "失败"));
        assertThrows(BusinessException.class, () -> service.chat(new AgentChatRequest("查询"), user));
        verify(redis, never()).setIfLockOwner(anyString(), anyString(), anyString(), anyString(), any());
        verify(redis).releaseLock(eq("academic:agent:busy:1"), anyString());
    }

    @Test void rejectsConcurrentRequestWithoutReleasingAnotherOwner() {
        when(redis.acquireLock(anyString(), anyString(), any())).thenReturn(false);
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.chat(new AgentChatRequest("查询"), user)).getStatus());
        verifyNoInteractions(agent);
        verify(redis, never()).releaseLock(anyString(), anyString());
    }

    @Test void rateLimitPreventsModelCall() {
        when(redis.increment(anyString(), any())).thenReturn(11L);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(BusinessException.class,
                () -> service.chat(new AgentChatRequest("查询"), user)).getStatus());
        verifyNoInteractions(agent);
    }

    @Test void trimsWholeTurns() throws Exception {
        String id = null;
        for (int i = 0; i < 10; i++) id = service.chat(new AgentChatRequest("问题" + i, id), user).conversationId();
        var history = mapper.readTree(data.get("academic:agent:conversation:1:" + id));
        assertEquals(16, history.size());
        assertEquals("问题2", history.get(0).path("content").asText());
    }

    @Test void staleWorkerCannotOverwriteHistoryOrReleaseNewOwnersLock() {
        String id = service.chat(new AgentChatRequest("第一轮"), user).conversationId();
        String key = "academic:agent:conversation:1:" + id;
        String previous = data.get(key);
        when(agent.chat(any(), any(), anyList())).thenAnswer(i -> {
            data.put("academic:agent:busy:1", "new-owner");
            return new AgentChatResponse("迟到的回答", List.of());
        });
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> service.chat(new AgentChatRequest("第二轮", id), user)).getStatus());
        assertEquals(previous, data.get(key));
        assertEquals("new-owner", data.get("academic:agent:busy:1"));
    }

    @Test void redisWriteFailureDoesNotReportSuccessfulAnswer() {
        when(redis.setIfLockOwner(anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("connection failed"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(BusinessException.class,
                () -> service.chat(new AgentChatRequest("查询"), user)).getStatus());
        verify(redis).releaseLock(eq("academic:agent:busy:1"), anyString());
    }
}

package com.academic.planning.service;

import com.academic.planning.common.BusinessException;
import com.academic.planning.dto.AgentChatRequest;
import com.academic.planning.security.AuthException;
import com.academic.planning.security.SessionUser;
import com.academic.planning.service.impl.AgentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final LlmClient llm = mock(LlmClient.class);
    private final SchoolService schools = mock(SchoolService.class);
    private final RecommendationService recommendations = mock(RecommendationService.class);
    private final SessionUser user = new SessionUser(1, "token", List.of("recommend:use", "school:read"), List.of());

    private AgentService service() {
        return new AgentServiceImpl(llm, schools, recommendations, mapper,
                Validation.buildDefaultValidatorFactory().getValidator(), new SkillLoader(mapper));
    }

    private com.fasterxml.jackson.databind.JsonNode call(String name, String args) {
        return mapper.valueToTree(Map.of("role", "assistant", "tool_calls", List.of(Map.of(
                "id", "call_1", "type", "function", "function", Map.of("name", name, "arguments", args)))));
    }

    @Test void bindsPerspectiveWithoutTriggerWordsAndPreservesHistory() {
        List<com.fasterxml.jackson.databind.JsonNode> history = List.of(
                mapper.valueToTree(Map.of("role", "user", "content", "我是福建物理类，580分，15000位")),
                mapper.valueToTree(Map.of("role", "assistant", "content", "你希望在哪个地区学习？")));
        when(llm.complete(anyList(), any())).thenAnswer(invocation -> {
            List<com.fasterxml.jackson.databind.JsonNode> messages = invocation.getArgument(0);
            String system = messages.get(0).path("content").asText();
            assertTrue(system.contains("张雪峰视角"));
            assertTrue(system.contains("就业倒推"));
            assertTrue(system.contains("不得保证录取"));
            assertTrue(system.contains("没有 WebSearch"));
            assertEquals(history.get(0), messages.get(1));
            assertEquals(history.get(1), messages.get(2));
            assertEquals("偏好江浙沪", messages.get(3).path("content").asText());
            com.fasterxml.jackson.databind.JsonNode definitions = invocation.getArgument(1);
            assertEquals(4, definitions.size());
            assertTrue(definitions.toString().contains("read_skill_resource"));
            return mapper.valueToTree(Map.of("role", "assistant", "content", "还需要了解你的专业偏好。"));
        });
        var result = service().chat(new AgentChatRequest("偏好江浙沪"), user, history);
        assertTrue(result.sources().isEmpty());
        assertEquals("zhangxuefeng-skill", result.skill().name());
        assertTrue(result.skill().instructionsInjected());
        assertTrue(result.skill().resourcesRead().isEmpty());
        verify(llm).complete(anyList(), any());
        verifyNoInteractions(schools, recommendations);
    }

    @Test void readsReferenceThenQueriesBusinessDataInSameLoop() {
        String path = "references/research/04-external-views.md";
        var query = call("search_schools", "{\"keyword\":\"厦门\"}");
        ((com.fasterxml.jackson.databind.node.ObjectNode) query.path("tool_calls").get(0)).put("id", "call_2");
        when(schools.list("厦门", null, 10)).thenReturn(List.of());
        when(llm.complete(anyList(), any()))
                .thenReturn(call("read_skill_resource", "{\"path\":\"" + path + "\"}"))
                .thenAnswer(invocation -> {
                    List<com.fasterxml.jackson.databind.JsonNode> messages = invocation.getArgument(0);
                    var data = mapper.readTree(messages.get(3).path("content").asText());
                    assertEquals("background_reference", data.path("kind").asText());
                    assertEquals(path, data.path("path").asText());
                    assertFalse(data.path("content").asText().isBlank());
                    return query;
                })
                .thenReturn(mapper.valueToTree(Map.of("role", "assistant", "content", "没有查询到学校数据。")));
        var result = service().chat(new AgentChatRequest("分析择校方法的局限，并查厦门的学校"), user);
        assertEquals(List.of("read_skill_resource", "search_schools"),
                result.sources().stream().map(s -> s.tool()).toList());
        assertEquals(List.of(path), result.skill().resourcesRead());
        verify(schools).list("厦门", null, 10);
    }

    @Test void readingReferenceRequiresConsultationPermissionButNotSchoolPermission() {
        var limited = new SessionUser(1, "token", List.of("recommend:use"), List.of());
        when(llm.complete(anyList(), any()))
                .thenReturn(call("read_skill_resource", "{\"path\":\"references/research/01-writings.md\"}"))
                .thenReturn(mapper.valueToTree(Map.of("role", "assistant", "content", "先明确目标，再比较成本和路径。")));
        assertEquals(1, service().chat(new AgentChatRequest("怎么考虑专业选择"), limited).sources().size());
        verifyNoInteractions(schools, recommendations);
        clearInvocations(llm);
        var unauthorized = new SessionUser(2, "token", List.of(), List.of());
        assertThrows(AuthException.class, () -> service().chat(new AgentChatRequest("怎么考虑专业选择"), unauthorized));
        verifyNoInteractions(llm);
    }

    @Test void rejectsInvalidReferenceToolArguments() {
        for (String args : List.of("{}", "{\"path\":2}", "{\"path\":\"../../.env\"}",
                "{\"path\":\"UPSTREAM-SKILL.md\"}",
                "{\"path\":\"references/research/01-writings.md\",\"command\":\"run\"}")) {
            when(llm.complete(anyList(), any())).thenReturn(call("read_skill_resource", args));
            assertThrows(BusinessException.class, () -> service().chat(new AgentChatRequest("分析"), user));
        }
        verifyNoInteractions(schools, recommendations);
    }

    @Test void completesToolLoopAndReturnsSources() {
        when(schools.list("厦门", null, 10)).thenReturn(List.of());
        when(llm.complete(anyList(), any())).thenReturn(call("search_schools", "{\"keyword\":\"厦门\"}"))
                .thenAnswer(invocation -> {
                    List<com.fasterxml.jackson.databind.JsonNode> messages = invocation.getArgument(0);
                    assertEquals("tool", messages.get(3).path("role").asText());
                    assertEquals("call_1", messages.get(3).path("tool_call_id").asText());
                    return mapper.valueToTree(Map.of("role", "assistant", "content", "没有查询到数据"));
                });
        var result = service().chat(new AgentChatRequest("查询厦门学校"), user);
        assertEquals("没有查询到数据", result.answer());
        assertEquals(1, result.sources().size());
        verify(schools).list("厦门", null, 10);
    }

    @Test void checksToolPermissionBeforeQuery() {
        when(llm.complete(anyList(), any())).thenReturn(call("school_detail", "{\"school_id\":1}"));
        var limited = new SessionUser(1, "token", List.of("recommend:use"), List.of());
        assertThrows(AuthException.class, () -> service().chat(new AgentChatRequest("查学校"), limited));
        verifyNoInteractions(schools);
    }

    @Test void rejectsMissingRankInsteadOfInventingDefault() {
        when(llm.complete(anyList(), any())).thenReturn(call("recommend",
                "{\"province\":\"福建\",\"subject_type\":\"物理类\",\"score\":580}"));
        assertThrows(BusinessException.class, () -> service().chat(new AgentChatRequest("推荐"), user));
        verifyNoInteractions(recommendations);
    }

    @Test void refusesUnknownToolAndInvalidJson() {
        for (var response : List.of(call("shell", "{}"), call("recommend", "not json"))) {
            when(llm.complete(anyList(), any())).thenReturn(response);
            assertThrows(BusinessException.class, () -> service().chat(new AgentChatRequest("推荐"), user));
        }
        verifyNoInteractions(schools, recommendations);
    }

    @Test void boundsToolLoop() {
        var index = new java.util.concurrent.atomic.AtomicInteger();
        when(llm.complete(anyList(), any())).thenAnswer(invocation -> {
            var result = call("search_schools", "{}");
            ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("tool_calls").get(0))
                    .put("id", "call_" + index.incrementAndGet());
            return result;
        });
        assertThrows(BusinessException.class, () -> service().chat(new AgentChatRequest("查询"), user));
        verify(llm, times(4)).complete(anyList(), any());
        verify(schools, times(3)).list(null, null, 10);
    }

    @Test void passesValidatedRecommendationToServiceAndKeepsProviderContinuationFields() {
        var response = new com.academic.planning.vo.RecommendationResponseVO("没有匹配数据",
                Map.of("冲", List.of(), "稳", List.of(), "保", List.of()), "仅供参考");
        when(recommendations.recommend(any())).thenReturn(response);
        var toolCall = call("recommend", "{\"province\":\"福建\",\"subject_type\":\"物理类\",\"score\":580,\"rank\":23000,\"major_preference\":\"计算机\"}");
        ((com.fasterxml.jackson.databind.node.ObjectNode) toolCall).put("reasoning_content", "provider continuation");
        when(llm.complete(anyList(), any())).thenReturn(toolCall).thenAnswer(i -> {
            List<com.fasterxml.jackson.databind.JsonNode> messages = i.getArgument(0);
            assertEquals("provider continuation", messages.get(2).path("reasoning_content").asText());
            assertEquals(mapper.valueToTree(response), mapper.readTree(messages.get(3).path("content").asText()));
            return mapper.valueToTree(Map.of("role", "assistant", "content", "当前没有匹配数据。"));
        });
        var result = service().chat(new AgentChatRequest("福建物理580分23000位计算机"), user);
        assertSame(response, result.sources().get(0).data());
        verify(recommendations).recommend(argThat(r -> r.rank() == 23000 && r.score() == 580
                && "物理类".equals(r.subjectType()) && "计算机".equals(r.majorPreference())));
    }

    @Test void rejectsOverlargeToolBatchBeforeExecutingQueries() {
        var message = mapper.createObjectNode().put("role", "assistant");
        var calls = message.putArray("tool_calls");
        for (int i = 0; i < 7; i++) {
            var item = call("search_schools", "{}").path("tool_calls").get(0).deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) item).put("id", "call_" + i);
            calls.add(item);
        }
        when(llm.complete(anyList(), any())).thenReturn(message);
        assertThrows(BusinessException.class, () -> service().chat(new AgentChatRequest("查询"), user));
        verifyNoInteractions(schools, recommendations);
    }

    @Test void rejectsDuplicateToolCallIdsAcrossRounds() {
        when(schools.list(null, null, 10)).thenReturn(List.of());
        when(llm.complete(anyList(), any())).thenReturn(call("search_schools", "{}"));
        assertThrows(BusinessException.class, () -> service().chat(new AgentChatRequest("查询"), user));
        verify(schools, times(1)).list(null, null, 10);
    }
}

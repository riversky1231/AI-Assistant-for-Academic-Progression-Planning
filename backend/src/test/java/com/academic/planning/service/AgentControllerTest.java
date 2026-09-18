package com.academic.planning.service;

import com.academic.planning.common.GlobalExceptionHandler;
import com.academic.planning.controller.AgentController;
import com.academic.planning.mapper.PermissionMapper;
import com.academic.planning.security.AuthInterceptor;
import com.academic.planning.vo.AgentChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AgentControllerTest {
    private final AgentConversationService service = mock(AgentConversationService.class);
    private final RedisGateway redis = mock(RedisGateway.class);
    private final PermissionMapper permissions = mock(PermissionMapper.class);
    private MockMvc mvc;

    @BeforeEach void setup() {
        var mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mvc = MockMvcBuilders.standaloneSetup(new AgentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .addInterceptors(new AuthInterceptor(redis, permissions)).build();
        when(redis.get("academic:session:test")).thenReturn("1");
        when(permissions.selectPermissionCodesByUserId(1)).thenReturn(List.of("recommend:use"));
        when(permissions.selectRoleCodesByUserId(1)).thenReturn(List.of());
    }

    @Test void requiresAuthenticationAndPermission() throws Exception {
        mvc.perform(post("/agent/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"你好\"}"))
                .andExpect(status().isUnauthorized());
        when(permissions.selectPermissionCodesByUserId(1)).thenReturn(List.of());
        mvc.perform(post("/agent/chat").header("satoken", "test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"你好\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void validatesMessageConversationAndMalformedJson() throws Exception {
        for (String body : List.of("{\"message\":\" \"}", "{}", "{\"message\":\"" + "a".repeat(4001) + "\"}",
                "{\"message\":\"你好\",\"conversation_id\":\"invalid\"}",
                "{\"message\":\"你好\",\"conversationId\":\"invalid\"}", "{")) {
            mvc.perform(post("/agent/chat").header("satoken", "test").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test void returnsSnakeCaseConversationId() throws Exception {
        String id = "12345678-1234-1234-1234-123456789abc";
        when(service.chat(any(), any())).thenReturn(new AgentChatResponse("你好", List.of(), id));
        mvc.perform(post("/agent/chat").header("satoken", "test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"继续\",\"conversation_id\":\"" + id + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.conversation_id").value(id));
        verify(service).chat(argThat(r -> id.equals(r.conversationId())), argThat(u -> u.userId() == 1));
    }

    @Test void acceptsCamelCaseConversationIdWithoutStartingNewConversation() throws Exception {
        String id = "98d66530-256b-772c-4f25-716f9eb5edca";
        when(service.chat(any(), any())).thenReturn(new AgentChatResponse("请补充科类", List.of(), id));
        mvc.perform(post("/agent/chat").header("satoken", "test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"福建580分23000位\",\"conversationId\":\"" + id + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.conversation_id").value(id))
                .andExpect(jsonPath("$.data.conversationId").doesNotExist());
        verify(service).chat(argThat(r -> id.equals(r.conversationId())), any());
    }
}

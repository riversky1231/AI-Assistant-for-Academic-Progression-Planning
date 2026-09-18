package com.academic.planning.service;

import com.academic.planning.dto.AgentChatRequest;
import com.academic.planning.security.SessionUser;
import com.academic.planning.vo.AgentChatResponse;

public interface AgentService {
    AgentChatResponse chat(AgentChatRequest request, SessionUser user);
    AgentChatResponse chat(AgentChatRequest request, SessionUser user,
                           java.util.List<com.fasterxml.jackson.databind.JsonNode> history);
}

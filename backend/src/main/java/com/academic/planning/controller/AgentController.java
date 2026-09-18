package com.academic.planning.controller;

import com.academic.planning.common.ApiResponse;
import com.academic.planning.dto.AgentChatRequest;
import com.academic.planning.security.AuthContext;
import com.academic.planning.security.RequirePermission;
import com.academic.planning.service.AgentConversationService;
import com.academic.planning.vo.AgentChatResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Agent 咨询", description = "固定张雪峰视角、多轮会话与业务工具查询")
public class AgentController {
    private final AgentConversationService service;

    public AgentController(AgentConversationService service) { this.service = service; }

    @PostMapping("/agent/chat")
    @Operation(summary = "发送一轮升学咨询", description = "需要 satoken 和 recommend:use 权限；查询学校另需 school:read。非流式 JSON 响应。Skill 指令每轮注入，sources 为空不代表 Skill 未加载。")
    @RequirePermission("recommend:use")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        return ApiResponse.success(service.chat(request, AuthContext.requireUser()));
    }
}

package com.academic.planning.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "升学咨询请求；首次省略 conversation_id，续聊使用上次响应中的会话 ID")
public record AgentChatRequest(
        @Schema(description = "本轮用户问题，1–4000 个字符且不能全为空白", example = "我是福建物理类考生，580分，位次23000，想学计算机")
        @NotBlank @Size(max = 4000) String message,
        @JsonProperty("conversation_id") @JsonAlias("conversationId")
        @Schema(name = "conversation_id", description = "可选的小写 UUID；必须属于当前用户且未过期。兼容 conversationId 输入，建议统一使用 conversation_id")
        @jakarta.validation.constraints.Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        String conversationId) {
    public AgentChatRequest(String message) { this(message, null); }
}

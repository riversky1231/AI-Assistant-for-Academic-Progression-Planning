package com.academic.planning.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record AgentChatResponse(
        @Schema(description = "本轮回答或追问，可能包含 Markdown") String answer,
        @Schema(description = "本轮实际执行的工具结果，允许为空；不包含历史轮次") List<ToolResult> sources,
        @Schema(description = "服务端生成的会话 ID，后续请求原样携带") String conversationId,
        @Schema(description = "本轮 Skill 指令注入及参考资料读取情况，不代表模型遵从度") SkillUsage skill) {
    public AgentChatResponse(String answer, List<ToolResult> sources) { this(answer, sources, null, null); }
    public AgentChatResponse(String answer, List<ToolResult> sources, String conversationId) {
        this(answer, sources, conversationId, null);
    }
    public record ToolResult(String tool, Object data) {}
    /** Reports context delivery, not a claim that the model followed every instruction. */
    public record SkillUsage(String name, String entry, boolean instructionsInjected,
                             String instructionsSha256, List<String> resourcesRead) {}
}

package com.academic.planning.service;

import com.academic.planning.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Fixed, packaged skill. The model can only read resources in the reviewed index. */
@Component
public class SkillLoader {
    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);
    public static final String NAME = "zhangxuefeng-skill";
    public static final String ENTRY = "skills/zhangxuefeng-skill/SKILL.md";
    private static final String ROOT = "/skills/zhangxuefeng-skill/";
    private static final int MAX_FILE_BYTES = 64 * 1024;
    private final String instructions;
    private final String instructionsSha256;
    private final Map<String, SkillResource> resources;

    public SkillLoader(ObjectMapper mapper) {
        try {
            // 读入口 SKILL.md 并剥离 YAML frontmatter，得到纯指令正文
            String entry = read("SKILL.md").replaceFirst("(?s)^---\\R.*?\\R---\\R", "").strip();
            if (entry.isBlank()) throw new IOException("Empty skill entry");
            // 读资源白名单索引，限制数量（1~16 条）
            var index = mapper.readTree(read("resource-index.json"));
            if (!index.isArray() || index.isEmpty() || index.size() > 16) {
                throw new IOException("Invalid skill resource index");
            }
            Map<String, SkillResource> loaded = new LinkedHashMap<>();
            StringBuilder catalog = new StringBuilder("\n\n可用背景资料（使用 read_skill_resource 按路径读取）：\n");
            for (var item : index) {
                String path = item.path("path").asText();
                String description = item.path("description").asText();
                // 路径必须严格落在 references/research 或 examples 下的 .md，防止越权读任意文件
                if (!path.matches("(?:references/research|examples)/[a-zA-Z0-9_-]+\\.md")
                        || description.isBlank() || loaded.containsKey(path)) {
                    throw new IOException("Invalid skill resource entry");
                }
                loaded.put(path, new SkillResource(path, description, "background_reference", read(path)));
                catalog.append("- ").append(path).append("：").append(description).append('\n');
            }
            resources = Collections.unmodifiableMap(loaded);
            // 最终指令 = 入口正文 + 资料目录；哈希用于部署版本核对
            instructions = entry + catalog;
            instructionsSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(instructions.getBytes(StandardCharsets.UTF_8)));
            log.info("Fixed skill loaded: name={}, entry={}, instructionsSha256={}, resourceCount={}",
                    NAME, ENTRY, instructionsSha256, resources.size());
        } catch (IOException | NoSuchAlgorithmException exception) {
            // 必需文件缺失/非法会导致启动失败，保证 Skill 始终可用
            throw new IllegalStateException("Cannot load required zhangxuefeng skill", exception);
        }
    }

    public String instructions() {
        return instructions;
    }

    public String instructionsSha256() {
        return instructionsSha256;
    }

    public SkillResource readResource(String path) {
        SkillResource resource = resources.get(path);
        if (resource == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型请求了不可用的 Skill 资料");
        }
        return resource;
    }

    private String read(String path) throws IOException {
        try (var input = SkillLoader.class.getResourceAsStream(ROOT + path)) {
            if (input == null) throw new IOException("Missing skill resource: " + path);
            byte[] bytes = input.readNBytes(MAX_FILE_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_FILE_BYTES) {
                throw new IOException("Invalid skill resource size: " + path);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public record SkillResource(String path, String description, String kind, String content) {}
}

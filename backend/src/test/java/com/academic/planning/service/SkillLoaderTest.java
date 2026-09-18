package com.academic.planning.service;

import com.academic.planning.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillLoaderTest {
    private final SkillLoader skill = new SkillLoader(new ObjectMapper());

    @Test void loadsPackagedEntryAndIndexesAllResourcesWithoutInliningThem() {
        String prompt = skill.instructions();
        assertFalse(prompt.startsWith("---"));
        assertTrue(prompt.contains("就业倒推"));
        assertTrue(prompt.contains("read_skill_resource"));
        for (String path : List.of("references/research/01-writings.md", "references/research/02-conversations.md",
                "references/research/03-expression-dna.md", "references/research/04-external-views.md",
                "references/research/05-decisions.md", "references/research/06-timeline.md",
                "examples/demo-conversation.md")) {
            assertTrue(prompt.contains(path));
            var resource = skill.readResource(path);
            assertEquals(path, resource.path());
            assertEquals("background_reference", resource.kind());
            assertFalse(resource.content().isBlank());
            assertFalse(prompt.contains(resource.content()));
        }
    }

    @Test void onlyReadsExactAllowlistedPaths() {
        for (String path : List.of("../application.yml", "../../.env", "/etc/passwd", "C:\\Windows\\win.ini",
                "references/research/../research/01-writings.md", "references\\research\\01-writings.md",
                "references/research/%2e%2e/.env", "https://example.com/file", "SKILL.md", "UPSTREAM-SKILL.md",
                "references/research/missing.md", "")) {
            assertThrows(BusinessException.class, () -> skill.readResource(path), path);
        }
    }
}

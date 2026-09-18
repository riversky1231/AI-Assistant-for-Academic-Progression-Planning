package com.academic.planning.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LlmConfigurationTest {
    @Test void springCreatesConfiguredClientProperties() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(LlmConfiguration.class)
                .withPropertyValues("app.llm.enabled=true", "app.llm.base-url=http://127.0.0.1/v1",
                        "app.llm.api-key=test-file-key", "app.llm.model=test-model", "app.llm.timeout-seconds=12")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var properties = context.getBean(LlmProperties.class);
                    assertTrue(properties.enabled());
                    assertEquals("test-model", properties.model());
                    assertEquals(12, properties.timeoutSeconds());
                    // Never print an actual process key in assertion diagnostics.
                    assertTrue(LlmConfiguration.resolveApiKey(System.getenv("LLM_API_KEY"), "test-file-key")
                            .equals(properties.apiKey()));
                });
    }
    @Test void environmentKeyOverridesFileAndIsTrimmed() {
        assertEquals("environment-key", LlmConfiguration.resolveApiKey(" environment-key ", "old-file-key"));
        assertEquals("file-key", LlmConfiguration.resolveApiKey(null, "file-key"));
        assertEquals("", LlmConfiguration.resolveApiKey("", "old-file-key"));
    }

    @Test void propertiesNeverPrintKey() {
        assertFalse(new LlmProperties(true, "", "private-key", "", 30).toString().contains("private-key"));
    }
}

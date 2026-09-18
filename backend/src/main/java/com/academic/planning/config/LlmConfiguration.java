package com.academic.planning.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LlmConfiguration {
    private static final Logger log = LoggerFactory.getLogger(LlmConfiguration.class);

    @Bean
    public LlmProperties llmProperties(Environment environment) {
        String processKey = System.getenv("LLM_API_KEY");
        String key = resolveApiKey(processKey, environment.getProperty("app.llm.api-key", ""));
        log.info("LLM API key: loaded={}, source={}", !key.isBlank(),
                processKey != null ? "process environment LLM_API_KEY" : "application configuration");
        return new LlmProperties(environment.getProperty("app.llm.enabled", Boolean.class, false),
                environment.getProperty("app.llm.base-url", ""), key,
                environment.getProperty("app.llm.model", ""),
                environment.getProperty("app.llm.timeout-seconds", Integer.class, 30));
    }

    static String resolveApiKey(String processKey, String configuredKey) {
        // An explicitly empty environment variable disables fallback to a stale file key.
        return (processKey != null ? processKey : configuredKey).trim();
    }
}

package com.academic.planning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.llm")
public record LlmProperties(@DefaultValue("false") boolean enabled,
                            @DefaultValue("") String baseUrl,
                            @DefaultValue("") String apiKey,
                            @DefaultValue("") String model,
                            @DefaultValue("30") int timeoutSeconds,
                            @DefaultValue("2048") int maxOutputTokens) {
    public LlmProperties(boolean enabled, String baseUrl, String apiKey, String model, int timeoutSeconds) {
        this(enabled, baseUrl, apiKey, model, timeoutSeconds, 2048);
    }
    @Override
    public String toString() {
        return "LlmProperties[enabled=" + enabled + ", apiKey=<redacted>, timeoutSeconds=" + timeoutSeconds + "]";
    }
}

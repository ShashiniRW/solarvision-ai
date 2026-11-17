package com.solarvision.backend.anomaly;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Connection to the Python AI service. The key has no real default on purpose:
 * without it, the AI features are skipped instead of calling the service unprotected.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiServiceProperties(
    @DefaultValue("http://localhost:8000") String baseUrl,
    @DefaultValue("") String internalApiKey,
    @DefaultValue("3s") Duration connectTimeout,
    @DefaultValue("10s") Duration readTimeout,
    // Chat answers take 5-15 s (search + Gemini), so they get a longer limit
    @DefaultValue("45s") Duration chatReadTimeout) {
}

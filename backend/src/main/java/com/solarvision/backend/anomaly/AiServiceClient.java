package com.solarvision.backend.anomaly;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/** Calls the internal Python AI service, with timeouts and the shared key. */
@Component
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiServiceClient {

    private final RestClient restClient;
    private final boolean configured;

    @Autowired
    public AiServiceClient(AiServiceProperties properties) {
        this(buildRestClient(properties, properties.readTimeout()), !properties.internalApiKey().isBlank());
    }

    // used by tests
    AiServiceClient(RestClient restClient, boolean configured) {
        this.restClient = restClient;
        this.configured = configured;
    }

    /** Shared by every client of the AI service; each one chooses how long to wait for an answer. */
    public static RestClient buildRestClient(AiServiceProperties properties, Duration readTimeout) {
        // HTTP/1.1 on purpose: on plain http://, Java would first try an HTTP/2 "upgrade",
        // which Uvicorn (the Python server) does not support, and the request body gets lost
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(properties.connectTimeout())
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader("X-Internal-Api-Key", properties.internalApiKey())
            .build();
    }

    /** False when no key is configured: callers should skip the AI features. */
    public boolean isConfigured() {
        return configured;
    }

    public PerformanceApi.Report analysePerformance(List<PerformanceApi.DayEnergy> days) {
        PerformanceApi.Report report = restClient.post()
            .uri("/v1/anomalies/performance")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new PerformanceApi.Request(days))
            .retrieve()
            .body(PerformanceApi.Report.class);

        if (report == null || report.findings() == null) {
            throw new IllegalStateException("AI service returned an empty report");
        }
        return report;
    }
}

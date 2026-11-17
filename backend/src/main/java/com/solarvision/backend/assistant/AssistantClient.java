package com.solarvision.backend.assistant;

import com.solarvision.backend.anomaly.AiServiceClient;
import com.solarvision.backend.anomaly.AiServiceProperties;
import com.solarvision.backend.common.exception.AssistantUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Sends chat questions to the AI service, with the longer chat timeout. */
@Component
public class AssistantClient {

    private final RestClient restClient;
    private final boolean configured;

    @Autowired
    public AssistantClient(AiServiceProperties properties) {
        this(AiServiceClient.buildRestClient(properties, properties.chatReadTimeout()),
            !properties.internalApiKey().isBlank());
    }

    // used by tests
    AssistantClient(RestClient restClient, boolean configured) {
        this.restClient = restClient;
        this.configured = configured;
    }

    public AssistantApi.ChatResponse chat(AssistantApi.ChatRequest request) {
        if (!configured) {
            throw new AssistantUnavailableException("app.ai.internal-api-key is not configured", null);
        }

        AssistantApi.ChatResponse response;
        try {
            response = restClient.post()
                .uri("/v1/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AssistantApi.ChatResponse.class);
        } catch (RestClientException ex) {
            // Error statuses from the AI service, timeouts and connection problems all land here
            throw new AssistantUnavailableException("AI service call failed: " + ex.getClass().getSimpleName(), ex);
        }

        if (response == null || response.reply() == null) {
            throw new AssistantUnavailableException("AI service returned an empty answer", null);
        }
        return response;
    }
}

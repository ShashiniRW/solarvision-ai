package com.solarvision.backend.assistant;

import com.solarvision.backend.common.exception.AssistantUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AssistantClientTest {

    private static final String CHAT_URL = "http://ai-service/v1/assistant/chat";

    private MockRestServiceServer server;
    private AssistantClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AssistantClient(builder.build(), true);
    }

    @Test
    void sendsTheQuestionAndReturnsTheAnswerWithSources() {
        server.expect(requestTo(CHAT_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.message").value("Inverter too hot?"))
            .andExpect(jsonPath("$.history").isEmpty())
            .andRespond(withSuccess("""
                {"reply": "Check the fans [1].", "model": "gemini-4-hydrogen",
                 "sources": [{"document": "SolarVision User Guide", "title": "Alert types"}]}
                """, MediaType.APPLICATION_JSON));

        AssistantApi.ChatResponse response =
            client.chat(new AssistantApi.ChatRequest("Inverter too hot?", null));

        assertThat(response.reply()).isEqualTo("Check the fans [1].");
        assertThat(response.sources()).extracting(AssistantApi.Source::title).containsExactly("Alert types");
        server.verify();
    }

    @Test
    void anErrorFromTheAiServiceBecomesAssistantUnavailable() {
        server.expect(requestTo(CHAT_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.chat(new AssistantApi.ChatRequest("Hi", List.of())))
            .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void doesNotCallTheAiServiceWithoutAKey() {
        AssistantClient unconfigured = new AssistantClient(RestClient.create(), false);

        assertThatThrownBy(() -> unconfigured.chat(new AssistantApi.ChatRequest("Hi", List.of())))
            .isInstanceOf(AssistantUnavailableException.class)
            .hasMessageContaining("not configured");
    }
}

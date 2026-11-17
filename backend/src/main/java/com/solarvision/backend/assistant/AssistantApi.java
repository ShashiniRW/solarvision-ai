package com.solarvision.backend.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** JSON shapes of the assistant chat, used by the browser API and by the call to the AI service. */
public final class AssistantApi {

    private AssistantApi() {
    }

    /** One earlier turn of the conversation. "model" is the assistant (Gemini's name for it). */
    public record ChatMessage(
        @NotBlank @Pattern(regexp = "user|model", message = "must be 'user' or 'model'") String role,
        @NotBlank @Size(max = 4000) String text) {
    }

    /** Same limits as the AI service, so mistakes are caught here with a clear 400. */
    public record ChatRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 20) List<@Valid ChatMessage> history) {

        public ChatRequest {
            history = history == null ? List.of() : history;
        }
    }

    /** A guide section the answer is based on. */
    public record Source(String document, String title) {
    }

    public record ChatResponse(String reply, String model, List<Source> sources) {

        public ChatResponse {
            sources = sources == null ? List.of() : sources;
        }
    }
}

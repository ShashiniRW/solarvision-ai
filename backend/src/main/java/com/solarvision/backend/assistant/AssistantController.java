package com.solarvision.backend.assistant;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantClient assistantClient;

    // Any signed-in user may ask. The browser never sees the AI service or its key.
    @PostMapping("/chat")
    public AssistantApi.ChatResponse chat(@Valid @RequestBody AssistantApi.ChatRequest request) {
        return assistantClient.chat(request);
    }
}

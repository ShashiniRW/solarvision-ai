package com.solarvision.backend.realtime;

import com.solarvision.backend.access.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class LiveEventController {

    private final LiveEventRegistry registry;
    private final AccessService accessService;

    /** Any logged-in user. Their project scope is taken now and decides what they receive. */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return registry.register(accessService.currentScope());
    }
}

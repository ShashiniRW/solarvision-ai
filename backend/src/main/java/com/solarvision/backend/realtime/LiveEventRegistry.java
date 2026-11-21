package com.solarvision.backend.realtime;

import com.solarvision.backend.access.ProjectScope;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the open browser connections and sends each event only to users whose
 * project scope allows it. In memory = correct for ONE backend instance
 * (several instances would share events through Redis).
 */
@Slf4j
@Component
public class LiveEventRegistry {

    // the browser reconnects by itself after this, so connections never live forever
    private static final long CONNECTION_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    private static final long RECONNECT_DELAY_MS = 5_000;

    private final Map<SseEmitter, ProjectScope> clients = new ConcurrentHashMap<>();

    public SseEmitter register(ProjectScope scope) {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> clients.remove(emitter));
        clients.put(emitter, scope);

        // first message: tells the browser to wait 5 s before reconnecting, and opens the stream at once
        send(emitter, SseEmitter.event().reconnectTime(RECONNECT_DELAY_MS).comment("connected"));
        return emitter;
    }

    /**
     * Runs AFTER the transaction that published the event commits, so the browser
     * never refreshes before the new data is in the database. fallbackExecution:
     * events published outside a transaction (the simulator) are sent immediately.
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onLiveEvent(LiveEvent event) {
        clients.forEach((emitter, scope) -> {
            if (scope.allows(event.projectId())) {
                send(emitter, SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
            }
        });
    }

    /** A tiny comment every 25 s keeps the connection open through proxies and firewalls. */
    @Scheduled(fixedRate = 25_000)
    public void sendHeartbeat() {
        clients.keySet().forEach(emitter -> send(emitter, SseEmitter.event().comment("ping")));
    }

    public int connectedClients() {
        return clients.size();
    }

    /** Lets the backend stop quickly instead of waiting for open connections. */
    @PreDestroy
    public void closeAll() {
        clients.keySet().forEach(SseEmitter::complete);
        clients.clear();
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            // the browser has gone away; the server cleans up the connection itself
            clients.remove(emitter);
            log.debug("Dropped a live-events connection: {}", ex.getMessage());
        }
    }
}

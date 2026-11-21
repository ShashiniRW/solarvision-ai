package com.solarvision.backend.realtime;

/**
 * A small message pushed to browsers. projectId decides WHO may receive it.
 * severity and message are only filled for ALERT_CREATED.
 */
public record LiveEvent(LiveEventType type, Long projectId, String severity, String message) {
}

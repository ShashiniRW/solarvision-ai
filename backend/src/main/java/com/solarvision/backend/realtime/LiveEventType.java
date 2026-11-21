package com.solarvision.backend.realtime;

/** What happened. The browser uses this to decide what to do. */
public enum LiveEventType {
    ALERT_CREATED,
    ALERT_UPDATED,      // acknowledged or resolved
    READINGS_RECORDED
}

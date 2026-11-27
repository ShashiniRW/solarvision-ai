package com.solarvision.backend.alert;

/** How many alerts of one severity (used by reports). */
public record AlertSeverityCount(AlertSeverity severity, Long count) {
}

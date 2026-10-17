package com.solarvision.backend.alert;

/**
 * A problem found by the rules. It becomes a real Alert only if there is
 * not already an unresolved alert of the same type for the same equipment.
 */
public record AlertCandidate(AlertType type, AlertSeverity severity, String message) {
}
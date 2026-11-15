package com.solarvision.backend.anomaly;

/** What one anomaly detection run did (returned by the "run now" endpoint, also logged). */
public record AnomalyRunSummary(int equipmentChecked, int alertsCreated, int failures,
                                String skippedReason) {

    static AnomalyRunSummary skipped(String reason) {
        return new AnomalyRunSummary(0, 0, 0, reason);
    }
}

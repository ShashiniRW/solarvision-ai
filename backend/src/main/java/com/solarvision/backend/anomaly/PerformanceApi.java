package com.solarvision.backend.anomaly;

import java.util.List;

/** Request and response of POST /v1/anomalies/performance on the AI service. */
public final class PerformanceApi {

    private PerformanceApi() {
    }

    /** Dates are ISO strings ("2026-11-14"); missing values are null, never 0. */
    public record DayEnergy(String date, Double expectedKwh, Double actualKwh) {
    }

    public record Request(List<DayEnergy> days) {
    }

    public record Finding(String date, String type, String severity,
                          double performanceRatio, String message) {
    }

    public record Report(String status, int daysAnalysed, Double baselineRatio,
                         List<Finding> findings) {
    }
}

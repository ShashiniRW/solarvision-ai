package com.solarvision.backend.report;

import java.math.BigDecimal;
import java.util.List;

/** Everything on the monthly report page (and in the CSV). */
public record MonthlyReportResponse(
    Long projectId,
    String projectName,
    String month,                 // "2026-11"
    BigDecimal totalEnergyKwh,
    BigDecimal averageDailyKwh,   // over the days that have readings; null when none have
    ReportDay bestDay,            // null when no day has readings
    List<ReportDay> days,         // every day of the month, in order
    AlertTotals alerts,
    InspectionTotals inspections
) {
}

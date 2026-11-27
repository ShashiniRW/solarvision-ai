package com.solarvision.backend.report;

import java.math.BigDecimal;

/** One day of a report. energyKwh is null when there were no readings that day (unknown, not 0). */
public record ReportDay(String date, BigDecimal energyKwh) {
}

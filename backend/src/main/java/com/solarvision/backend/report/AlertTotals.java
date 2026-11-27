package com.solarvision.backend.report;

/** Alerts raised in the report period. "resolved" = of those, how many are resolved now. */
public record AlertTotals(long total, long critical, long warning, long resolved) {
}

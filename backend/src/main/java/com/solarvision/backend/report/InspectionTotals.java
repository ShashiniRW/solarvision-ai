package com.solarvision.backend.report;

/** Inspections planned in the period, and inspections completed in the period. */
public record InspectionTotals(long scheduled, long completed) {
}

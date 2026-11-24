package com.solarvision.backend.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
    long openAlerts,
    long acknowledgedAlerts,
    long unresolvedCriticalAlerts,
    long scheduledInspections,
    long inProgressInspections,
    long overdueInspections,
    long equipmentTotal,
    long equipmentActive,
    long equipmentFaulty,
    BigDecimal energyTodayKwh,
    BigDecimal currentPowerKw, // newest reading of every inverter that reported in the last 15 minutes
    BigDecimal capacityKw      // total capacity of the active inverters
) {
}

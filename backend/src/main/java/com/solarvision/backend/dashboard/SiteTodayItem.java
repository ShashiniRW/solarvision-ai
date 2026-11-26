package com.solarvision.backend.dashboard;

import com.solarvision.backend.project.dto.SiteMapItem.Health;

import java.math.BigDecimal;

/** One row of the dashboard's "Sites today" table. */
public record SiteTodayItem(
    Long siteId,
    String siteName,
    Long projectId,
    String projectName,
    BigDecimal capacityKw,      // installed capacity (null when unknown)
    BigDecimal energyTodayKwh,  // since midnight, Colombo time
    BigDecimal currentPowerKw,  // newest inverter readings from the last 15 minutes
    long criticalAlerts,
    long warningAlerts,
    Health health
) {
}

package com.solarvision.backend.project.dto;

import java.math.BigDecimal;

/**
 * One site on the map. Latitude and longitude may be null: such sites are listed
 * under the map instead of being hidden ("missing is not zero").
 */
public record SiteMapItem(
    Long id,
    String name,
    Long projectId,
    String projectName,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal capacityKw,
    long equipmentCount,
    long criticalAlerts,
    long warningAlerts,
    Health health) {

    /** The site's worst unresolved alert decides its colour on the map. */
    public enum Health {
        OK,
        WARNING,
        CRITICAL
    }
}

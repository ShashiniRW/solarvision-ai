package com.solarvision.backend.equipment;

/** Number of pieces of equipment at one site (a row of a GROUP BY query). */
public record SiteEquipmentCount(Long siteId, Long count) {
}

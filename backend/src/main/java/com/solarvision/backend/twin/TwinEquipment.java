package com.solarvision.backend.twin;

import com.solarvision.backend.project.dto.SiteMapItem.Health;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** One piece of equipment in the 3D twin: what it is, how healthy it is, and its latest reading. */
public record TwinEquipment(
    Long id,
    String equipmentType,
    String serialNumber,
    String model,
    BigDecimal capacityKw,
    String status,
    Health health,
    BigDecimal powerKw,          // null = no readings yet
    BigDecimal temperatureC,
    OffsetDateTime lastReadingAt
) {
}

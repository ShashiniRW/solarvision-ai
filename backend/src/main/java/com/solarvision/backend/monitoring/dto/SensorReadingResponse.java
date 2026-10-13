package com.solarvision.backend.monitoring.dto;

import com.solarvision.backend.monitoring.SensorReading;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SensorReadingResponse(
        Long id,
        Long equipmentId,
        OffsetDateTime recordedAt,
        BigDecimal powerKw,
        BigDecimal energyKwh,
        BigDecimal voltageV,
        BigDecimal currentA,
        BigDecimal temperatureC,
        Boolean isSimulated
) {
    public static SensorReadingResponse fromEntity(SensorReading r) {
        return new SensorReadingResponse(
                r.getId(),
                r.getEquipment().getId(),
                r.getRecordedAt(),
                r.getPowerKw(),
                r.getEnergyKwh(),
                r.getVoltageV(),
                r.getCurrentA(),
                r.getTemperatureC(),
                r.getIsSimulated()
        );
    }
}
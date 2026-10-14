package com.solarvision.backend.monitoring.dto;

import com.solarvision.backend.monitoring.DailySummaryProjection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyEnergySummaryResponse(
        LocalDate date,
        BigDecimal totalEnergyKwh,
        BigDecimal peakPowerKw,
        BigDecimal avgTemperatureC,
        Long readingCount
) {
    public static DailyEnergySummaryResponse fromProjection(DailySummaryProjection p) {
        return new DailyEnergySummaryResponse(
                LocalDate.parse(p.getDay()),
                p.getTotalEnergyKwh(),
                p.getPeakPowerKw(),
                p.getAvgTemperatureC(),
                p.getReadingCount()
        );
    }
}
package com.solarvision.backend.weather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Weather and expected solar energy for one site, one entry per day.
 * expectedEnergyKwh = capacityKw × sunHours × performanceRatio
 */
public record SiteForecastResponse(
    Long siteId,
    BigDecimal capacityKw,
    BigDecimal performanceRatio,
    String attribution,
    List<Day> days) {

    /**
     * @param sunHours          sunlight on a horizontal surface in kWh/m² ("peak sun hours"), null if unknown
     * @param expectedEnergyKwh null when sunlight or the site capacity is unknown
     */
    public record Day(
        LocalDate date,
        Integer weatherCode,
        Double temperatureMaxC,
        Double temperatureMinC,
        Integer precipitationProbabilityPercent,
        BigDecimal sunHours,
        BigDecimal expectedEnergyKwh) {
    }
}

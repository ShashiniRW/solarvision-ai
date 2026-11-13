package com.solarvision.backend.weather;

import java.time.LocalDate;

/**
 * One day of weather for a location, in our own units.
 * Any value can be null when the provider has no data for it.
 *
 * @param radiationKwhPerM2 total sunlight on a horizontal surface (= "peak sun hours")
 */
public record DailyWeather(
    LocalDate date,
    Double radiationKwhPerM2,
    Double temperatureMaxC,
    Double temperatureMinC,
    Integer weatherCode,
    Integer precipitationProbabilityPercent) {
}

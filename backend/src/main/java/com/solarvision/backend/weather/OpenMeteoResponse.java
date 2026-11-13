package com.solarvision.backend.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** The part of Open-Meteo's /v1/forecast JSON that we read. Each list has one value per day. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoResponse(Daily daily) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
        List<String> time,
        @JsonProperty("shortwave_radiation_sum") List<Double> shortwaveRadiationSum, // MJ/m²
        @JsonProperty("temperature_2m_max") List<Double> temperatureMax,
        @JsonProperty("temperature_2m_min") List<Double> temperatureMin,
        @JsonProperty("weather_code") List<Integer> weatherCode,
        @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbabilityMax) {
    }
}

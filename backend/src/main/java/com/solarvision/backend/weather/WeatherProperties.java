package com.solarvision.backend.weather;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Settings for the weather provider (Open-Meteo). Every value has a safe default,
 * and each can be overridden in application.properties, e.g. app.weather.read-timeout=15s
 */
@ConfigurationProperties(prefix = "app.weather")
public record WeatherProperties(
    @DefaultValue("https://api.open-meteo.com") String baseUrl,
    @DefaultValue("5s") Duration connectTimeout,
    @DefaultValue("10s") Duration readTimeout,
    @DefaultValue("Asia/Colombo") String timezone) {
}

package com.solarvision.backend.weather;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Tells Spring to read the app.weather.* settings into WeatherProperties. */
@Configuration
@EnableConfigurationProperties(WeatherProperties.class)
public class WeatherConfig {
}

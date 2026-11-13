package com.solarvision.backend.weather;

import com.solarvision.backend.common.exception.WeatherUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads daily weather from Open-Meteo (free for non-commercial use, data under CC BY 4.0).
 * Any failure (network, timeout, HTTP error, bad data) becomes a WeatherUnavailableException.
 */
@Component
public class WeatherClient {

    private static final String DAILY_VALUES = String.join(",",
        "shortwave_radiation_sum",
        "temperature_2m_max",
        "temperature_2m_min",
        "weather_code",
        "precipitation_probability_max");

    // Open-Meteo gives sunlight in MJ/m²; 1 kWh = 3.6 MJ
    private static final double MJ_PER_KWH = 3.6;

    private final RestClient restClient;
    private final String timezone;

    @Autowired
    public WeatherClient(WeatherProperties properties) {
        this(buildRestClient(properties), properties.timezone());
    }

    // used by tests, which pass in a RestClient connected to a fake server
    WeatherClient(RestClient restClient, String timezone) {
        this.restClient = restClient;
        this.timezone = timezone;
    }

    private static RestClient buildRestClient(WeatherProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout())
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    /**
     * Daily weather from {@code pastDays} days ago until {@code forecastDays - 1} days ahead
     * (days counted in the configured time zone).
     */
    public List<DailyWeather> fetchDaily(BigDecimal latitude, BigDecimal longitude,
                                         int pastDays, int forecastDays) {
        OpenMeteoResponse response;
        try {
            response = restClient.get()
                .uri(uri -> uri.path("/v1/forecast")
                    .queryParam("latitude", latitude.toPlainString())
                    .queryParam("longitude", longitude.toPlainString())
                    .queryParam("daily", DAILY_VALUES)
                    .queryParam("timezone", timezone)
                    .queryParam("past_days", pastDays)
                    .queryParam("forecast_days", forecastDays)
                    .build())
                .retrieve()
                .body(OpenMeteoResponse.class);
        } catch (RestClientException e) {
            throw new WeatherUnavailableException("Weather service is unavailable", e);
        }
        return toDailyWeather(response);
    }

    private List<DailyWeather> toDailyWeather(OpenMeteoResponse response) {
        if (response == null || response.daily() == null || response.daily().time() == null) {
            throw new WeatherUnavailableException("Weather service returned no daily data");
        }

        OpenMeteoResponse.Daily daily = response.daily();
        int days = daily.time().size();

        // every list must have exactly one value per day, otherwise the days would be mixed up
        if (!allHaveSize(days,
            daily.shortwaveRadiationSum(),
            daily.temperatureMax(),
            daily.temperatureMin(),
            daily.weatherCode(),
            daily.precipitationProbabilityMax())) {
            throw new WeatherUnavailableException("Weather service returned inconsistent daily data");
        }

        List<DailyWeather> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            Double radiationMj = daily.shortwaveRadiationSum().get(i);
            result.add(new DailyWeather(
                LocalDate.parse(daily.time().get(i)),
                radiationMj == null ? null : radiationMj / MJ_PER_KWH,
                daily.temperatureMax().get(i),
                daily.temperatureMin().get(i),
                daily.weatherCode().get(i),
                daily.precipitationProbabilityMax().get(i)));
        }
        return List.copyOf(result);
    }

    private static boolean allHaveSize(int expected, List<?>... lists) {
        for (List<?> list : lists) {
            if (list == null || list.size() != expected) {
                return false;
            }
        }
        return true;
    }
}

package com.solarvision.backend.weather;

import com.solarvision.backend.common.exception.WeatherUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherClientTest {

    private static final BigDecimal LATITUDE = new BigDecimal("6.9147");
    private static final BigDecimal LONGITUDE = new BigDecimal("79.8481");

    // two days, like Open-Meteo's real answer (extra fields such as "latitude" must be ignored)
    private static final String TWO_DAYS = """
            {
              "latitude": 6.924429,
              "daily_units": { "shortwave_radiation_sum": "MJ/m²" },
              "daily": {
                "time": ["2026-11-12", "2026-11-13"],
                "shortwave_radiation_sum": [18.0, null],
                "temperature_2m_max": [30.5, 29.0],
                "temperature_2m_min": [24.0, 23.5],
                "weather_code": [3, 95],
                "precipitation_probability_max": [20, 100]
              }
            }
            """;

    private MockRestServiceServer server;
    private WeatherClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://weather.test");
        server = MockRestServiceServer.bindTo(builder).build(); // fake server, no internet
        client = new WeatherClient(builder.build(), "Asia/Colombo");
    }

    @Test
    void sendsLocationTimezoneAndDayRange() {
        server.expect(requestTo(startsWith("https://weather.test/v1/forecast")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("latitude", "6.9147"))
            .andExpect(queryParam("longitude", "79.8481"))
            .andExpect(queryParam("timezone", "Asia/Colombo"))
            .andExpect(queryParam("past_days", "7"))
            .andExpect(queryParam("forecast_days", "7"))
            .andRespond(withSuccess(TWO_DAYS, MediaType.APPLICATION_JSON));

        client.fetchDaily(LATITUDE, LONGITUDE, 7, 7);

        server.verify();
    }

    @Test
    void convertsSunlightFromMegajoulesToKilowattHours() {
        server.expect(requestTo(startsWith("https://weather.test/v1/forecast")))
            .andRespond(withSuccess(TWO_DAYS, MediaType.APPLICATION_JSON));

        List<DailyWeather> days = client.fetchDaily(LATITUDE, LONGITUDE, 0, 2);

        assertThat(days).hasSize(2);

        DailyWeather first = days.get(0);
        assertThat(first.date()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(first.radiationKwhPerM2()).isCloseTo(5.0, within(0.0001)); // 18 MJ / 3.6
        assertThat(first.temperatureMaxC()).isEqualTo(30.5);
        assertThat(first.temperatureMinC()).isEqualTo(24.0);
        assertThat(first.weatherCode()).isEqualTo(3);
        assertThat(first.precipitationProbabilityPercent()).isEqualTo(20);

        // a missing value stays missing (null), it is not turned into 0
        assertThat(days.get(1).radiationKwhPerM2()).isNull();
    }

    @Test
    void serverErrorBecomesWeatherUnavailable() {
        server.expect(requestTo(startsWith("https://weather.test/v1/forecast")))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchDaily(LATITUDE, LONGITUDE, 0, 2))
            .isInstanceOf(WeatherUnavailableException.class)
            .hasMessage("Weather service is unavailable");
    }

    @Test
    void listsOfDifferentLengthsAreRejected() {
        String broken = """
                {
                  "daily": {
                    "time": ["2026-11-12", "2026-11-13"],
                    "shortwave_radiation_sum": [18.0],
                    "temperature_2m_max": [30.5, 29.0],
                    "temperature_2m_min": [24.0, 23.5],
                    "weather_code": [3, 95],
                    "precipitation_probability_max": [20, 100]
                  }
                }
                """;
        server.expect(requestTo(startsWith("https://weather.test/v1/forecast")))
            .andRespond(withSuccess(broken, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchDaily(LATITUDE, LONGITUDE, 0, 2))
            .isInstanceOf(WeatherUnavailableException.class)
            .hasMessage("Weather service returned inconsistent daily data");
    }

    @Test
    void missingDailyDataIsRejected() {
        server.expect(requestTo(startsWith("https://weather.test/v1/forecast")))
            .andRespond(withSuccess("{ \"latitude\": 6.92 }", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchDaily(LATITUDE, LONGITUDE, 0, 2))
            .isInstanceOf(WeatherUnavailableException.class)
            .hasMessage("Weather service returned no daily data");
    }
}

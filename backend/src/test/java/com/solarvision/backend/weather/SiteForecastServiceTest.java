package com.solarvision.backend.weather;

import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.common.exception.WeatherUnavailableException;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.project.SiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteForecastServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    private SiteService siteService;
    private WeatherClient weatherClient;
    private TestClock clock;
    private SiteForecastService service;

    @BeforeEach
    void setUp() {
        siteService = mock(SiteService.class);
        weatherClient = mock(WeatherClient.class);
        clock = new TestClock(Instant.parse("2026-11-12T04:00:00Z"));
        service = new SiteForecastService(siteService, weatherClient, new BigDecimal("0.80"), clock);
    }

    @Test
    void expectedEnergyIsCapacityTimesSunHoursTimesPerformanceRatio() {
        givenSite(new BigDecimal("10.50"));
        givenWeather(5.0);

        SiteForecastResponse forecast = service.getForecast(1L);

        SiteForecastResponse.Day day = forecast.days().get(0);
        assertThat(day.date()).isEqualTo(DAY);
        assertThat(day.sunHours()).isEqualByComparingTo("5.00");
        assertThat(day.expectedEnergyKwh()).isEqualByComparingTo("42.00"); // 10.5 × 5 × 0.8
        assertThat(day.weatherCode()).isEqualTo(3);
        assertThat(forecast.siteId()).isEqualTo(1L);
        assertThat(forecast.attribution()).contains("Open-Meteo");
    }

    @Test
    void unknownSunlightGivesNoExpectedEnergy() {
        givenSite(new BigDecimal("10.50"));
        givenWeather(null);

        SiteForecastResponse.Day day = service.getForecast(1L).days().get(0);

        assertThat(day.sunHours()).isNull();
        assertThat(day.expectedEnergyKwh()).isNull();
        assertThat(day.temperatureMaxC()).isEqualTo(30.5); // the rest of the weather is still shown
    }

    @Test
    void siteWithoutCapacityGivesSunHoursButNoExpectedEnergy() {
        givenSite(null);
        givenWeather(5.0);

        SiteForecastResponse.Day day = service.getForecast(1L).days().get(0);

        assertThat(day.sunHours()).isEqualByComparingTo("5.00");
        assertThat(day.expectedEnergyKwh()).isNull();
    }

    @Test
    void siteWithoutLocationIsRejectedWithoutCallingTheProvider() {
        Site site = givenSite(new BigDecimal("10.50"));
        site.setLatitude(null);

        assertThatThrownBy(() -> service.getForecast(1L))
            .isInstanceOf(ConflictException.class);
        verify(weatherClient, never()).fetchDaily(any(), any(), anyInt(), anyInt());
    }

    @Test
    void hiddenOrMissingSiteGives404WithoutCallingTheProvider() {
        when(siteService.getSiteById(1L)).thenThrow(new ResourceNotFoundException("Site not found"));

        assertThatThrownBy(() -> service.getForecast(1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Site not found");
        verify(weatherClient, never()).fetchDaily(any(), any(), anyInt(), anyInt());
    }

    @Test
    void weatherIsCachedFor30Minutes() {
        givenSite(new BigDecimal("10.50"));
        givenWeather(5.0);

        service.getForecast(1L);
        clock.advance(Duration.ofMinutes(29));
        service.getForecast(1L);
        verify(weatherClient, times(1)).fetchDaily(any(), any(), anyInt(), anyInt()); // second call from cache

        clock.advance(Duration.ofMinutes(2)); // now 31 minutes after the first call
        service.getForecast(1L);
        verify(weatherClient, times(2)).fetchDaily(any(), any(), anyInt(), anyInt()); // cache expired
    }

    @Test
    void failuresAreNotCached() {
        givenSite(new BigDecimal("10.50"));
        when(weatherClient.fetchDaily(any(), any(), anyInt(), anyInt()))
            .thenThrow(new WeatherUnavailableException("Weather service is unavailable"))
            .thenReturn(List.of(weather(5.0)));

        assertThatThrownBy(() -> service.getForecast(1L))
            .isInstanceOf(WeatherUnavailableException.class);

        // the very next request tries the provider again and succeeds
        assertThat(service.getForecast(1L).days()).hasSize(1);
        verify(weatherClient, times(2)).fetchDaily(any(), any(), anyInt(), anyInt());
    }

    @Test
    void nonsensePerformanceRatioStopsTheApp() {
        assertThatThrownBy(() ->
            new SiteForecastService(siteService, weatherClient, new BigDecimal("5"), clock))
            .isInstanceOf(IllegalStateException.class);
    }

    // ---------- helpers ----------

    private Site givenSite(BigDecimal capacityKw) {
        Site site = new Site();
        site.setId(1L);
        site.setLatitude(new BigDecimal("6.914700"));
        site.setLongitude(new BigDecimal("79.848100"));
        site.setCapacityKw(capacityKw);
        when(siteService.getSiteById(1L)).thenReturn(site);
        return site;
    }

    private void givenWeather(Double radiationKwhPerM2) {
        when(weatherClient.fetchDaily(any(), any(), anyInt(), anyInt()))
            .thenReturn(List.of(weather(radiationKwhPerM2)));
    }

    private static DailyWeather weather(Double radiationKwhPerM2) {
        return new DailyWeather(DAY, radiationKwhPerM2, 30.5, 24.0, 3, 20);
    }

    /** A clock the test can move forward. */
    private static final class TestClock extends Clock {
        private Instant now;

        TestClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}

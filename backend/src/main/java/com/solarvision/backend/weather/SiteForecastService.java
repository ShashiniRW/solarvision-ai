package com.solarvision.backend.weather;

import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.project.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the forecast for a site: weather from Open-Meteo plus the expected energy per day.
 * Weather is cached per location for 30 minutes so page loads do not hit the provider each time.
 */
@Service
public class SiteForecastService {

    static final int PAST_DAYS = 7;
    static final int FORECAST_DAYS = 7;
    static final Duration CACHE_TIME = Duration.ofMinutes(30);
    static final String ATTRIBUTION = "Weather data by Open-Meteo.com (CC BY 4.0)";

    private final SiteService siteService;
    private final WeatherClient weatherClient;
    private final BigDecimal performanceRatio;
    private final Clock clock;

    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    private record CachedWeather(Instant fetchedAt, List<DailyWeather> days) {
    }

    @Autowired
    public SiteForecastService(SiteService siteService,
                               WeatherClient weatherClient,
                               @Value("${app.forecast.performance-ratio:0.80}") BigDecimal performanceRatio) {
        this(siteService, weatherClient, performanceRatio, Clock.systemUTC());
    }

    // used by tests, which control the clock to test the cache
    SiteForecastService(SiteService siteService, WeatherClient weatherClient,
                        BigDecimal performanceRatio, Clock clock) {
        if (performanceRatio.signum() <= 0 || performanceRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("app.forecast.performance-ratio must be above 0 and at most 1");
        }
        this.siteService = siteService;
        this.weatherClient = weatherClient;
        this.performanceRatio = performanceRatio;
        this.clock = clock;
    }

    public SiteForecastResponse getForecast(Long siteId) {
        // 404 "Site not found" when the site does not exist or the user may not see it
        Site site = siteService.getSiteById(siteId);

        if (site.getLatitude() == null || site.getLongitude() == null) {
            throw new ConflictException("This site has no location, so no forecast is available");
        }

        List<SiteForecastResponse.Day> days = weatherFor(site.getLatitude(), site.getLongitude()).stream()
            .map(day -> toDay(day, site.getCapacityKw()))
            .toList();

        return new SiteForecastResponse(site.getId(), site.getCapacityKw(), performanceRatio, ATTRIBUTION, days);
    }

    private List<DailyWeather> weatherFor(BigDecimal latitude, BigDecimal longitude) {
        String key = latitude.stripTrailingZeros().toPlainString()
            + "," + longitude.stripTrailingZeros().toPlainString();
        Instant now = clock.instant();

        CachedWeather cached = cache.get(key);
        if (cached != null && cached.fetchedAt().plus(CACHE_TIME).isAfter(now)) {
            return cached.days();
        }

        // if this throws, nothing is stored, so the next request tries again
        List<DailyWeather> fresh = weatherClient.fetchDaily(latitude, longitude, PAST_DAYS, FORECAST_DAYS);
        cache.put(key, new CachedWeather(now, fresh));
        return fresh;
    }

    private SiteForecastResponse.Day toDay(DailyWeather day, BigDecimal capacityKw) {
        Double radiation = day.radiationKwhPerM2();

        BigDecimal sunHours = radiation == null
            ? null
            : BigDecimal.valueOf(radiation).setScale(2, RoundingMode.HALF_UP);

        // multiply with the unrounded sunlight, round only the final result
        BigDecimal expectedEnergy = (radiation == null || capacityKw == null)
            ? null
            : capacityKw.multiply(BigDecimal.valueOf(radiation))
            .multiply(performanceRatio)
            .setScale(2, RoundingMode.HALF_UP);

        return new SiteForecastResponse.Day(
            day.date(),
            day.weatherCode(),
            day.temperatureMaxC(),
            day.temperatureMinC(),
            day.precipitationProbabilityPercent(),
            sunHours,
            expectedEnergy);
    }
}

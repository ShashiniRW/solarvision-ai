package com.solarvision.backend.dashboard;

import com.solarvision.backend.monitoring.SensorReadingRepository;
import com.solarvision.backend.monitoring.SiteValue;
import com.solarvision.backend.project.SiteMapService;
import com.solarvision.backend.project.dto.SiteMapItem;
import com.solarvision.backend.project.dto.SiteMapItem.Health;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Energy today and live power for every site the current user may see,
 * problem sites first. Three queries in total, however many sites there are.
 */
@Service
@RequiredArgsConstructor
public class SitesTodayService {

    // critical sites on top, then warning, then OK; same health = alphabetical
    private static final Comparator<SiteTodayItem> PROBLEMS_FIRST =
        Comparator.comparingInt((SiteTodayItem site) -> rank(site.health()))
            .thenComparing(SiteTodayItem::siteName, String.CASE_INSENSITIVE_ORDER);

    private final SiteMapService siteMapService;
    private final SensorReadingRepository sensorReadingRepository;

    @Transactional(readOnly = true)
    public List<SiteTodayItem> getSitesToday() {
        // already filtered by the access rules, and already has health + alert counts
        List<SiteMapItem> sites = siteMapService.getMapSites();
        if (sites.isEmpty()) {
            return List.of();
        }
        Set<Long> siteIds = sites.stream().map(SiteMapItem::id).collect(Collectors.toSet());

        ZonedDateTime startOfToday = DashboardService.startOfToday();
        Map<Long, BigDecimal> energyBySite = toMap(sensorReadingRepository.sumEnergyBetweenBySite(
            startOfToday.toOffsetDateTime(), startOfToday.plusDays(1).toOffsetDateTime(), siteIds));
        Map<Long, BigDecimal> powerBySite = toMap(sensorReadingRepository.sumLatestInverterPowerSinceBySite(
            DashboardService.liveSince(), siteIds));

        return sites.stream()
            .map(site -> new SiteTodayItem(
                site.id(),
                site.name(),
                site.projectId(),
                site.projectName(),
                site.capacityKw(),
                energyBySite.getOrDefault(site.id(), BigDecimal.ZERO),
                powerBySite.getOrDefault(site.id(), BigDecimal.ZERO),
                site.criticalAlerts(),
                site.warningAlerts(),
                site.health()))
            .sorted(PROBLEMS_FIRST)
            .toList();
    }

    private static Map<Long, BigDecimal> toMap(List<SiteValue> rows) {
        return rows.stream()
            .filter(row -> Objects.nonNull(row.value()))
            .collect(Collectors.toMap(SiteValue::siteId, SiteValue::value));
    }

    private static int rank(Health health) {
        return switch (health) {
            case CRITICAL -> 0;
            case WARNING -> 1;
            case OK -> 2;
        };
    }
}

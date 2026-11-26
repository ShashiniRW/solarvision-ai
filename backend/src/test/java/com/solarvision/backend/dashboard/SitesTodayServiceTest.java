package com.solarvision.backend.dashboard;

import com.solarvision.backend.monitoring.SensorReadingRepository;
import com.solarvision.backend.monitoring.SiteValue;
import com.solarvision.backend.project.SiteMapService;
import com.solarvision.backend.project.dto.SiteMapItem;
import com.solarvision.backend.project.dto.SiteMapItem.Health;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitesTodayServiceTest {

    @Mock
    private SiteMapService siteMapService;

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @InjectMocks
    private SitesTodayService service;

    private static SiteMapItem site(long id, String name, Health health) {
        return new SiteMapItem(id, name, 10L, "Project", null, null, null, 3L,
            health == Health.CRITICAL ? 1L : 0L,
            health == Health.WARNING ? 1L : 0L,
            health);
    }

    @Test
    void returnsEmptyListWithoutQueryingReadingsWhenUserHasNoSites() {
        when(siteMapService.getMapSites()).thenReturn(List.of());

        assertThat(service.getSitesToday()).isEmpty();
        verifyNoInteractions(sensorReadingRepository);
    }

    @Test
    void combinesEnergyAndPowerPerSiteAndUsesZeroWhenThereAreNoReadings() {
        when(siteMapService.getMapSites()).thenReturn(List.of(
            site(1L, "Roof A", Health.OK),
            site(2L, "Roof B", Health.OK)));
        when(sensorReadingRepository.sumEnergyBetweenBySite(any(), any(), any()))
            .thenReturn(List.of(new SiteValue(1L, new BigDecimal("12.50"))));
        when(sensorReadingRepository.sumLatestInverterPowerSinceBySite(any(), any()))
            .thenReturn(List.of(new SiteValue(1L, new BigDecimal("4.20"))));

        List<SiteTodayItem> result = service.getSitesToday();

        assertThat(result).extracting(SiteTodayItem::siteName).containsExactly("Roof A", "Roof B");
        assertThat(result.get(0).energyTodayKwh()).isEqualByComparingTo("12.50");
        assertThat(result.get(0).currentPowerKw()).isEqualByComparingTo("4.20");
        assertThat(result.get(1).energyTodayKwh()).isEqualByComparingTo("0");
        assertThat(result.get(1).currentPowerKw()).isEqualByComparingTo("0");
    }

    @Test
    void putsCriticalSitesFirstThenWarningThenOk() {
        when(siteMapService.getMapSites()).thenReturn(List.of(
            site(1L, "Alpha", Health.OK),
            site(2L, "Bravo", Health.WARNING),
            site(3L, "Charlie", Health.CRITICAL),
            site(4L, "Delta", Health.CRITICAL)));
        // the reading queries return empty lists (Mockito's default)

        List<SiteTodayItem> result = service.getSitesToday();

        assertThat(result).extracting(SiteTodayItem::siteName)
            .containsExactly("Charlie", "Delta", "Bravo", "Alpha");
    }
}

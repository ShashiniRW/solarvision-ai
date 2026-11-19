package com.solarvision.backend.project;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.alert.SiteAlertCount;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.equipment.SiteEquipmentCount;
import com.solarvision.backend.project.dto.SiteMapItem;
import com.solarvision.backend.project.dto.SiteMapItem.Health;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteMapServiceTest {

    @Mock
    private SiteRepository siteRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AccessService accessService;

    @InjectMocks
    private SiteMapService siteMapService;

    @Test
    void worstUnresolvedAlertDecidesTheHealthOfEachSite() {
        when(accessService.currentScope()).thenReturn(ProjectScope.everything());
        when(siteRepository.findAllWithProject()).thenReturn(List.of(
            site(1L, "Critical site", 10L),
            site(2L, "Warning site", 10L),
            site(3L, "Healthy site", 10L)));
        when(equipmentRepository.countBySite()).thenReturn(List.of(
            new SiteEquipmentCount(1L, 4L),
            new SiteEquipmentCount(2L, 2L)));
        when(alertRepository.countUnresolvedBySiteAndSeverity(AlertStatus.RESOLVED)).thenReturn(List.of(
            new SiteAlertCount(1L, AlertSeverity.CRITICAL, 1L),
            new SiteAlertCount(1L, AlertSeverity.WARNING, 3L),
            new SiteAlertCount(2L, AlertSeverity.WARNING, 2L)));

        List<SiteMapItem> items = siteMapService.getMapSites();

        assertThat(items).extracting(SiteMapItem::health)
            .containsExactly(Health.CRITICAL, Health.WARNING, Health.OK);
        assertThat(items.get(0).criticalAlerts()).isEqualTo(1);
        assertThat(items.get(0).warningAlerts()).isEqualTo(3);
        assertThat(items.get(0).equipmentCount()).isEqualTo(4);
        // a site with no equipment rows gets 0, not an error
        assertThat(items.get(2).equipmentCount()).isZero();
    }

    @Test
    void onlySitesOfAccessibleProjectsAreReturned() {
        when(accessService.currentScope()).thenReturn(ProjectScope.only(Set.of(10L)));
        when(siteRepository.findAllWithProject()).thenReturn(List.of(
            site(1L, "Mine", 10L),
            site(2L, "Someone else's", 20L)));

        List<SiteMapItem> items = siteMapService.getMapSites();

        assertThat(items).extracting(SiteMapItem::name).containsExactly("Mine");
    }

    @Test
    void noAccessibleSitesSkipsTheCountQueries() {
        when(accessService.currentScope()).thenReturn(ProjectScope.only(Set.of()));
        when(siteRepository.findAllWithProject()).thenReturn(List.of(site(1L, "Other", 20L)));

        assertThat(siteMapService.getMapSites()).isEmpty();
        verifyNoInteractions(equipmentRepository, alertRepository);
    }

    @Test
    void aSiteWithoutCoordinatesIsStillReturned() {
        when(accessService.currentScope()).thenReturn(ProjectScope.everything());
        Site noLocation = site(1L, "No GPS yet", 10L);
        noLocation.setLatitude(null);
        noLocation.setLongitude(null);
        when(siteRepository.findAllWithProject()).thenReturn(List.of(noLocation));

        List<SiteMapItem> items = siteMapService.getMapSites();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).latitude()).isNull();
    }

    private static Site site(Long id, String name, Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        project.setName("Project " + projectId);

        Site site = new Site();
        site.setId(id);
        site.setName(name);
        site.setProject(project);
        site.setLatitude(new BigDecimal("6.927079"));
        site.setLongitude(new BigDecimal("79.861244"));
        site.setCapacityKw(new BigDecimal("50.00"));
        return site;
    }
}

package com.solarvision.backend.twin;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.alert.EquipmentAlertCount;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentService;
import com.solarvision.backend.monitoring.SensorReading;
import com.solarvision.backend.monitoring.SensorReadingRepository;
import com.solarvision.backend.project.Project;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.project.SiteRepository;
import com.solarvision.backend.project.dto.SiteMapItem.Health;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteTwinServiceTest {

    private static final long SITE_ID = 1L;

    @Mock private SiteRepository siteRepository;
    @Mock private EquipmentService equipmentService;
    @Mock private AlertRepository alertRepository;
    @Mock private SensorReadingRepository sensorReadingRepository;
    @Mock private AccessService accessService;

    @InjectMocks
    private SiteTwinService service;

    private Site site() {
        Project project = new Project();
        project.setId(10L);
        Site site = new Site();
        site.setId(SITE_ID);
        site.setName("Galle Rooftop");
        site.setProject(project);
        return site;
    }

    private Equipment equipment(long id, String type, String status) {
        Equipment item = new Equipment();
        item.setId(id);
        item.setEquipmentType(type);
        item.setStatus(status);
        return item;
    }

    private void siteIsVisible() {
        when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site()));
        when(accessService.canAccessProject(10L)).thenReturn(true);
    }

    @Test
    void healthComesFromTheWorstUnresolvedAlertAndTheLatestReadingIsAttached() {
        siteIsVisible();
        Equipment panel = equipment(100L, "PANEL", "ACTIVE");
        Equipment inverter = equipment(101L, "INVERTER", "ACTIVE");
        Equipment battery = equipment(102L, "BATTERY", "ACTIVE");
        when(equipmentService.getEquipmentBySite(SITE_ID)).thenReturn(List.of(panel, inverter, battery));
        when(alertRepository.countUnresolvedByEquipmentForSite(SITE_ID, AlertStatus.RESOLVED)).thenReturn(List.of(
            new EquipmentAlertCount(100L, AlertSeverity.WARNING, 2L),
            new EquipmentAlertCount(100L, AlertSeverity.CRITICAL, 1L),
            new EquipmentAlertCount(101L, AlertSeverity.WARNING, 1L)));

        SensorReading reading = new SensorReading();
        reading.setEquipment(inverter);
        reading.setPowerKw(new BigDecimal("3.500"));
        reading.setTemperatureC(new BigDecimal("41.20"));
        reading.setRecordedAt(OffsetDateTime.now());
        when(sensorReadingRepository.findLatestForSite(SITE_ID)).thenReturn(List.of(reading));

        SiteTwinResponse twin = service.getTwin(SITE_ID);

        assertThat(twin.siteName()).isEqualTo("Galle Rooftop");
        assertThat(twin.equipment()).extracting(TwinEquipment::health)
            .containsExactly(Health.CRITICAL, Health.WARNING, Health.OK);
        assertThat(twin.equipment().get(1).powerKw()).isEqualByComparingTo("3.5");
        assertThat(twin.equipment().get(2).powerKw()).isNull(); // no readings yet
    }

    @Test
    void faultyEquipmentIsCriticalEvenWithoutAlerts() {
        siteIsVisible();
        when(equipmentService.getEquipmentBySite(SITE_ID)).thenReturn(List.of(equipment(100L, "INVERTER", "FAULTY")));
        when(alertRepository.countUnresolvedByEquipmentForSite(SITE_ID, AlertStatus.RESOLVED)).thenReturn(List.of());
        when(sensorReadingRepository.findLatestForSite(SITE_ID)).thenReturn(List.of());

        assertThat(service.getTwin(SITE_ID).equipment().getFirst().health()).isEqualTo(Health.CRITICAL);
    }

    @Test
    void siteOfAnotherProjectLooksLikeItDoesNotExist() {
        when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site()));
        when(accessService.canAccessProject(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.getTwin(SITE_ID)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(equipmentService, alertRepository, sensorReadingRepository);
    }

    @Test
    void missingSiteIs404() {
        when(siteRepository.findById(SITE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTwin(SITE_ID)).isInstanceOf(ResourceNotFoundException.class);
    }
}

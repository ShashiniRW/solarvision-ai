package com.solarvision.backend.dashboard;

import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.inspection.InspectionRepository;
import com.solarvision.backend.inspection.InspectionStatus;
import com.solarvision.backend.monitoring.SensorReadingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @InjectMocks
    private DashboardService service;

    @Test
    void getSummary_collectsTheCountsFromTheRepositories() {
        when(alertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(3L);
        when(alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED)).thenReturn(2L);
        when(alertRepository.countBySeverityAndStatusNot(AlertSeverity.CRITICAL, AlertStatus.RESOLVED))
                .thenReturn(1L);
        when(inspectionRepository.countByStatus(InspectionStatus.SCHEDULED)).thenReturn(5L);
        when(inspectionRepository.countByStatus(InspectionStatus.IN_PROGRESS)).thenReturn(4L);
        when(inspectionRepository.countByStatusAndScheduledAtBefore(
                any(InspectionStatus.class), any(OffsetDateTime.class))).thenReturn(6L);
        when(equipmentRepository.count()).thenReturn(10L);
        when(equipmentRepository.countByStatus("ACTIVE")).thenReturn(8L);
        when(equipmentRepository.countByStatus("FAULTY")).thenReturn(1L);
        when(sensorReadingRepository.sumEnergyBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(new BigDecimal("12.345"));

        DashboardSummaryResponse result = service.getSummary();

        assertThat(result.openAlerts()).isEqualTo(3);
        assertThat(result.acknowledgedAlerts()).isEqualTo(2);
        assertThat(result.unresolvedCriticalAlerts()).isEqualTo(1);
        assertThat(result.scheduledInspections()).isEqualTo(5);
        assertThat(result.inProgressInspections()).isEqualTo(4);
        assertThat(result.overdueInspections()).isEqualTo(6);
        assertThat(result.equipmentTotal()).isEqualTo(10);
        assertThat(result.equipmentActive()).isEqualTo(8);
        assertThat(result.equipmentFaulty()).isEqualTo(1);
        assertThat(result.energyTodayKwh()).isEqualByComparingTo("12.345");
    }

    @Test
    void getSummary_reportsZeroEnergy_whenThereAreNoReadingsToday() {
        when(sensorReadingRepository.sumEnergyBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(null);

        assertThat(service.getSummary().energyTodayKwh()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSummary_asksForExactlyOneDayOfEnergy_startingAtLocalMidnight() {
        when(sensorReadingRepository.sumEnergyBetween(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(BigDecimal.ONE);

        service.getSummary();

        ArgumentCaptor<OffsetDateTime> start = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> end = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(sensorReadingRepository).sumEnergyBetween(start.capture(), end.capture());

        // the range is one day long and begins at midnight Colombo time (+05:30)
        assertThat(end.getValue()).isEqualTo(start.getValue().plusDays(1));
        assertThat(start.getValue().getOffset().getTotalSeconds()).isEqualTo(5 * 3600 + 30 * 60);
        assertThat(start.getValue().getHour()).isZero();
        assertThat(start.getValue().getMinute()).isZero();
    }
}
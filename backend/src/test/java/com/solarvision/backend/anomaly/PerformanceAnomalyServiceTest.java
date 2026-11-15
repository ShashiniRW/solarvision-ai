package com.solarvision.backend.anomaly;

import com.solarvision.backend.alert.AlertService;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertType;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.monitoring.DailySummaryProjection;
import com.solarvision.backend.monitoring.SensorReadingRepository;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.weather.SiteForecastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PerformanceAnomalyServiceTest {

    // 15 Nov 2026, 09:30 in Colombo -> yesterday = 14 Nov, analysed days = 8..14 Nov
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T04:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 11, 14);

    private EquipmentRepository equipmentRepository;
    private SensorReadingRepository sensorReadingRepository;
    private SiteForecastService siteForecastService;
    private AiServiceClient aiServiceClient;
    private AlertService alertService;
    private PerformanceAnomalyService service;

    @BeforeEach
    void setUp() {
        equipmentRepository = mock(EquipmentRepository.class);
        sensorReadingRepository = mock(SensorReadingRepository.class);
        siteForecastService = mock(SiteForecastService.class);
        aiServiceClient = mock(AiServiceClient.class);
        alertService = mock(AlertService.class);
        service = new PerformanceAnomalyService(equipmentRepository, sensorReadingRepository,
            siteForecastService, aiServiceClient, alertService, CLOCK);

        when(aiServiceClient.isConfigured()).thenReturn(true);
        when(siteForecastService.performanceRatio()).thenReturn(new BigDecimal("0.80"));
        when(siteForecastService.sunHoursByDate(any(), any())).thenReturn(Map.of());
        when(sensorReadingRepository.findDailySummary(anyLong(), any(), any(), anyString()))
            .thenReturn(List.of());
    }

    @Test
    void skipsEverythingWhenNoKeyIsConfigured() {
        when(aiServiceClient.isConfigured()).thenReturn(false);

        AnomalyRunSummary summary = service.runDetection();

        assertThat(summary.skippedReason()).isNotNull();
        verifyNoInteractions(equipmentRepository, alertService);
    }

    @Test
    void expectedEnergyIsCapacityTimesSunHoursTimesRatio_andMissingDataStaysNull() {
        Equipment equipment = equipment(1L, "5");
        when(siteForecastService.sunHoursByDate(any(), any()))
            .thenReturn(Map.of(YESTERDAY, 4.0, YESTERDAY.minusDays(1), 5.0));
        DailySummaryProjection day = mock(DailySummaryProjection.class);
        when(day.getDay()).thenReturn("2026-11-14");
        when(day.getTotalEnergyKwh()).thenReturn(new BigDecimal("10.0"));
        when(sensorReadingRepository.findDailySummary(eq(1L), any(), any(), anyString()))
            .thenReturn(List.of(day));

        List<PerformanceApi.DayEnergy> days =
            service.buildDays(equipment, YESTERDAY.minusDays(6), YESTERDAY);

        assertThat(days).hasSize(7);
        PerformanceApi.DayEnergy last = days.get(6);
        assertThat(last.date()).isEqualTo("2026-11-14");
        assertThat(last.expectedKwh()).isEqualTo(16.0, org.assertj.core.data.Offset.offset(1e-9)); // 5 x 4 x 0.8
        assertThat(last.actualKwh()).isEqualTo(10.0);

        PerformanceApi.DayEnergy dayBefore = days.get(5);
        assertThat(dayBefore.expectedKwh()).isEqualTo(20.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(dayBefore.actualKwh()).isNull(); // no readings: unknown, not 0

        assertThat(days.get(0).expectedKwh()).isNull(); // no weather data
    }

    @Test
    void onlyFindingsForYesterdayBecomeAlerts() {
        when(equipmentRepository.findAllForPerformanceAnalysis()).thenReturn(List.of(equipment(1L, "5")));
        when(aiServiceClient.analysePerformance(any())).thenReturn(report(
            finding("2026-11-13", "UNUSUAL_DROP", "WARNING"),
            finding("2026-11-14", "LOW_PERFORMANCE", "CRITICAL")));
        when(alertService.raiseIfNoActiveAlert(anyLong(), any(), any(), anyString())).thenReturn(true);

        AnomalyRunSummary summary = service.runDetection();

        assertThat(summary.equipmentChecked()).isEqualTo(1);
        assertThat(summary.alertsCreated()).isEqualTo(1);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(alertService, times(1)).raiseIfNoActiveAlert(
            eq(1L), eq(AlertType.LOW_PERFORMANCE), eq(AlertSeverity.CRITICAL), message.capture());
        assertThat(message.getValue()).contains("2026-11-14");
        verify(alertService, never()).raiseIfNoActiveAlert(
            anyLong(), eq(AlertType.UNUSUAL_DROP), any(), anyString());
    }

    @Test
    void aFailureForOneEquipmentDoesNotStopTheOthers() {
        when(equipmentRepository.findAllForPerformanceAnalysis())
            .thenReturn(List.of(equipment(1L, "5"), equipment(2L, "5")));
        when(aiServiceClient.analysePerformance(any()))
            .thenThrow(new IllegalStateException("AI service down"))
            .thenReturn(report());

        AnomalyRunSummary summary = service.runDetection();

        assertThat(summary.failures()).isEqualTo(1);
        assertThat(summary.equipmentChecked()).isEqualTo(1);
    }

    // ---------- helpers ----------

    private static Equipment equipment(Long id, String capacityKw) {
        Site site = new Site();
        site.setLatitude(new BigDecimal("6.914700"));
        site.setLongitude(new BigDecimal("79.848100"));

        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setCapacityKw(new BigDecimal(capacityKw));
        equipment.setSite(site);
        return equipment;
    }

    private static PerformanceApi.Finding finding(String date, String type, String severity) {
        return new PerformanceApi.Finding(date, type, severity, 0.2, "test finding");
    }

    private static PerformanceApi.Report report(PerformanceApi.Finding... findings) {
        return new PerformanceApi.Report("OK", 7, 0.8, List.of(findings));
    }
}

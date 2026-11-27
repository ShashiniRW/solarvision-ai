package com.solarvision.backend.report;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertSeverityCount;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.inspection.InspectionRepository;
import com.solarvision.backend.inspection.InspectionStatus;
import com.solarvision.backend.monitoring.ProjectDailyEnergy;
import com.solarvision.backend.monitoring.SensorReadingRepository;
import com.solarvision.backend.project.Project;
import com.solarvision.backend.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private AccessService accessService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SensorReadingRepository sensorReadingRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private InspectionRepository inspectionRepository;

    @InjectMocks
    private ReportService service;

    private static ProjectDailyEnergy row(String day, String energyKwh) {
        return new ProjectDailyEnergy() {
            @Override
            public String getDay() {
                return day;
            }

            @Override
            public BigDecimal getEnergyKwh() {
                return new BigDecimal(energyKwh);
            }
        };
    }

    @Test
    void monthlyReport_fillsEveryDay_andAddsUpEnergyAlertsAndInspections() {
        Project project = new Project();
        project.setId(5L);
        project.setName("Hambantota Solar Farm");
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(sensorReadingRepository.sumEnergyPerDayForProject(eq(5L), any(), any(), eq("Asia/Colombo")))
            .thenReturn(List.of(row("2026-02-01", "10.50"), row("2026-02-03", "20.00")));
        when(alertRepository.countBySeverityInProjectBetween(eq(5L), any(), any()))
            .thenReturn(List.of(
                new AlertSeverityCount(AlertSeverity.CRITICAL, 1L),
                new AlertSeverityCount(AlertSeverity.WARNING, 2L)));
        when(alertRepository.countByStatusInProjectBetween(eq(5L), eq(AlertStatus.RESOLVED), any(), any()))
            .thenReturn(1L);
        when(inspectionRepository.countScheduledInProjectBetween(eq(5L), any(), any())).thenReturn(4L);
        when(inspectionRepository.countCompletedInProjectBetween(
            eq(5L), eq(InspectionStatus.COMPLETED), any(), any())).thenReturn(2L);

        MonthlyReportResponse report = service.monthlyReport(5L, YearMonth.of(2026, 2));

        assertThat(report.month()).isEqualTo("2026-02");
        assertThat(report.days()).hasSize(28);                     // every day of February 2026
        assertThat(report.days().get(1).energyKwh()).isNull();     // 2 Feb: no readings = unknown, not 0
        assertThat(report.totalEnergyKwh()).isEqualByComparingTo("30.50");
        assertThat(report.averageDailyKwh()).isEqualByComparingTo("15.25"); // over the 2 days with data
        assertThat(report.bestDay().date()).isEqualTo("2026-02-03");
        assertThat(report.alerts()).isEqualTo(new AlertTotals(3, 1, 2, 1));
        assertThat(report.inspections()).isEqualTo(new InspectionTotals(4, 2));
    }

    @Test
    void monthlyReport_refusesAProjectTheUserMayNotSee_withoutQueryingAnyData() {
        doThrow(new ResourceNotFoundException("Project not found"))
            .when(accessService).requireProjectAccess(9L);

        assertThatThrownBy(() -> service.monthlyReport(9L, YearMonth.of(2026, 11)))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(projectRepository, sensorReadingRepository, alertRepository, inspectionRepository);
    }
}

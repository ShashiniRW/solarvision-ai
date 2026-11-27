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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monthly report for one project: energy per day, alerts and inspections.
 * Three grouped queries; the database does the adding up.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    // the same "day" as the dashboard: days start at midnight in Colombo
    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");

    private final AccessService accessService;
    private final ProjectRepository projectRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AlertRepository alertRepository;
    private final InspectionRepository inspectionRepository;

    @Transactional(readOnly = true)
    public MonthlyReportResponse monthlyReport(Long projectId, YearMonth month) {
        // same access rules as everywhere: 404 for a project this user may not see
        accessService.requireProjectAccess(projectId);
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        OffsetDateTime rangeStart = month.atDay(1).atStartOfDay(SITE_ZONE).toOffsetDateTime();
        OffsetDateTime rangeEnd = month.plusMonths(1).atDay(1).atStartOfDay(SITE_ZONE).toOffsetDateTime();

        List<ReportDay> days = dailyEnergy(projectId, month, rangeStart, rangeEnd);
        List<ReportDay> daysWithData = days.stream().filter(day -> day.energyKwh() != null).toList();

        BigDecimal total = daysWithData.stream()
            .map(ReportDay::energyKwh)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = daysWithData.isEmpty()
            ? null
            : total.divide(BigDecimal.valueOf(daysWithData.size()), 2, RoundingMode.HALF_UP);
        ReportDay bestDay = daysWithData.stream()
            .max(Comparator.comparing(ReportDay::energyKwh))
            .orElse(null);

        return new MonthlyReportResponse(
            project.getId(),
            project.getName(),
            month.toString(),
            total,
            average,
            bestDay,
            days,
            alertTotals(projectId, rangeStart, rangeEnd),
            new InspectionTotals(
                inspectionRepository.countScheduledInProjectBetween(projectId, rangeStart, rangeEnd),
                inspectionRepository.countCompletedInProjectBetween(
                    projectId, InspectionStatus.COMPLETED, rangeStart, rangeEnd)));
    }

    /** Every day of the month in order; null energy = no readings that day. */
    private List<ReportDay> dailyEnergy(Long projectId, YearMonth month,
                                        OffsetDateTime rangeStart, OffsetDateTime rangeEnd) {
        Map<String, BigDecimal> energyByDay = new HashMap<>();
        for (ProjectDailyEnergy row : sensorReadingRepository.sumEnergyPerDayForProject(
            projectId, rangeStart, rangeEnd, SITE_ZONE.getId())) {
            if (row.getEnergyKwh() != null) {
                energyByDay.put(row.getDay(), row.getEnergyKwh());
            }
        }

        List<ReportDay> days = new ArrayList<>(month.lengthOfMonth());
        for (int dayOfMonth = 1; dayOfMonth <= month.lengthOfMonth(); dayOfMonth++) {
            String date = month.atDay(dayOfMonth).toString(); // "2026-11-01"
            days.add(new ReportDay(date, energyByDay.get(date)));
        }
        return days;
    }

    private AlertTotals alertTotals(Long projectId, OffsetDateTime rangeStart, OffsetDateTime rangeEnd) {
        Map<AlertSeverity, Long> bySeverity = new EnumMap<>(AlertSeverity.class);
        for (AlertSeverityCount row : alertRepository.countBySeverityInProjectBetween(
            projectId, rangeStart, rangeEnd)) {
            bySeverity.put(row.severity(), row.count());
        }
        long total = bySeverity.values().stream().mapToLong(Long::longValue).sum();

        return new AlertTotals(
            total,
            bySeverity.getOrDefault(AlertSeverity.CRITICAL, 0L),
            bySeverity.getOrDefault(AlertSeverity.WARNING, 0L),
            alertRepository.countByStatusInProjectBetween(
                projectId, AlertStatus.RESOLVED, rangeStart, rangeEnd));
    }
}

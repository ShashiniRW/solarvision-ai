package com.solarvision.backend.dashboard;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.inspection.InspectionRepository;
import com.solarvision.backend.inspection.InspectionStatus;
import com.solarvision.backend.monitoring.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    // For now every site uses one time zone; later this will come from the site
    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");

    private final AlertRepository alertRepository;
    private final InspectionRepository inspectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        ProjectScope scope = accessService.currentScope();

        if (scope.unrestricted()) {
            return summaryOfEverything();          // ADMIN
        }
        if (scope.projectIds().isEmpty()) {
            return emptySummary();                 // no projects: nothing to count
        }
        return summaryOfProjects(scope.projectIds());
    }

    // ---------- ADMIN: whole database ----------

    private DashboardSummaryResponse summaryOfEverything() {
        ZonedDateTime startOfToday = startOfToday();
        BigDecimal energyToday = sensorReadingRepository.sumEnergyBetween(
                startOfToday.toOffsetDateTime(),
                startOfToday.plusDays(1).toOffsetDateTime());

        return new DashboardSummaryResponse(
                alertRepository.countByStatus(AlertStatus.OPEN),
                alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED),
                alertRepository.countBySeverityAndStatusNot(AlertSeverity.CRITICAL, AlertStatus.RESOLVED),
                inspectionRepository.countByStatus(InspectionStatus.SCHEDULED),
                inspectionRepository.countByStatus(InspectionStatus.IN_PROGRESS),
                inspectionRepository.countByStatusAndScheduledAtBefore(
                        InspectionStatus.SCHEDULED, OffsetDateTime.now()),
                equipmentRepository.count(),
                equipmentRepository.countByStatus("ACTIVE"),
                equipmentRepository.countByStatus("FAULTY"),
                orZero(energyToday)
        );
    }

    // ---------- everyone else: only their projects ----------

    private DashboardSummaryResponse summaryOfProjects(Set<Long> projectIds) {
        ZonedDateTime startOfToday = startOfToday();
        BigDecimal energyToday = sensorReadingRepository.sumEnergyBetweenInProjects(
                startOfToday.toOffsetDateTime(),
                startOfToday.plusDays(1).toOffsetDateTime(),
                projectIds);

        return new DashboardSummaryResponse(
                alertRepository.countByStatusInProjects(AlertStatus.OPEN, projectIds),
                alertRepository.countByStatusInProjects(AlertStatus.ACKNOWLEDGED, projectIds),
                alertRepository.countBySeverityAndStatusNotInProjects(
                        AlertSeverity.CRITICAL, AlertStatus.RESOLVED, projectIds),
                inspectionRepository.countByStatusInProjects(InspectionStatus.SCHEDULED, projectIds),
                inspectionRepository.countByStatusInProjects(InspectionStatus.IN_PROGRESS, projectIds),
                inspectionRepository.countByStatusAndScheduledAtBeforeInProjects(
                        InspectionStatus.SCHEDULED, OffsetDateTime.now(), projectIds),
                equipmentRepository.countInProjects(projectIds),
                equipmentRepository.countByStatusInProjects("ACTIVE", projectIds),
                equipmentRepository.countByStatusInProjects("FAULTY", projectIds),
                orZero(energyToday)
        );
    }

    private DashboardSummaryResponse emptySummary() {
        return new DashboardSummaryResponse(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO);
    }

    // ---------- helpers ----------

    /** "today" is the current day in the site's time zone */
    private ZonedDateTime startOfToday() {
        return LocalDate.now(SITE_ZONE).atStartOfDay(SITE_ZONE);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
package com.solarvision.backend.dashboard;

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

@Service
@RequiredArgsConstructor
public class DashboardService {

    // For now every site uses one time zone; later this will come from the site
    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");

    private final AlertRepository alertRepository;
    private final InspectionRepository inspectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final SensorReadingRepository sensorReadingRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        // "today" is the current day in the site's time zone
        ZonedDateTime startOfToday = LocalDate.now(SITE_ZONE).atStartOfDay(SITE_ZONE);
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
                energyToday != null ? energyToday : BigDecimal.ZERO
        );
    }
}
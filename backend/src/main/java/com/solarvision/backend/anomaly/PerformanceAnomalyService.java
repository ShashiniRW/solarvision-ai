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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compares each equipment's actual energy with the weather-based expected energy
 * (via the AI service) and raises alerts for problems found on the previous day.
 */
@Slf4j
@Service
public class PerformanceAnomalyService {

    static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");
    static final int DAYS_ANALYSED = 7;

    private final EquipmentRepository equipmentRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final SiteForecastService siteForecastService;
    private final AiServiceClient aiServiceClient;
    private final AlertService alertService;
    private final Clock clock;

    @Autowired
    public PerformanceAnomalyService(EquipmentRepository equipmentRepository,
                                     SensorReadingRepository sensorReadingRepository,
                                     SiteForecastService siteForecastService,
                                     AiServiceClient aiServiceClient,
                                     AlertService alertService) {
        this(equipmentRepository, sensorReadingRepository, siteForecastService,
            aiServiceClient, alertService, Clock.systemUTC());
    }

    // used by tests, which fix "today"
    PerformanceAnomalyService(EquipmentRepository equipmentRepository,
                              SensorReadingRepository sensorReadingRepository,
                              SiteForecastService siteForecastService,
                              AiServiceClient aiServiceClient,
                              AlertService alertService,
                              Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.siteForecastService = siteForecastService;
        this.aiServiceClient = aiServiceClient;
        this.alertService = alertService;
        this.clock = clock;
    }

    public AnomalyRunSummary runDetection() {
        if (!aiServiceClient.isConfigured()) {
            log.warn("Performance anomaly detection skipped: app.ai.internal-api-key is not set");
            return AnomalyRunSummary.skipped("AI service key is not configured");
        }

        // today is not finished yet, so the last complete day is yesterday
        LocalDate today = LocalDate.now(clock.withZone(SITE_ZONE));
        LocalDate yesterday = today.minusDays(1);
        LocalDate from = today.minusDays(DAYS_ANALYSED);

        int checked = 0;
        int alertsCreated = 0;
        int failures = 0;

        for (Equipment equipment : equipmentRepository.findAllForPerformanceAnalysis()) {
            try {
                PerformanceApi.Report report =
                    aiServiceClient.analysePerformance(buildDays(equipment, from, yesterday));
                checked++;

                for (PerformanceApi.Finding finding : report.findings()) {
                    // only new problems: older days were handled by earlier runs
                    if (LocalDate.parse(finding.date()).equals(yesterday)
                        && raiseAlert(equipment.getId(), finding)) {
                        alertsCreated++;
                    }
                }
            } catch (RuntimeException e) {
                // one failing piece of equipment (weather or AI service down) must not stop the rest
                failures++;
                log.warn("Anomaly detection failed for equipment {}: {}",
                    equipment.getId(), e.getMessage());
            }
        }

        log.info("Anomaly detection finished: {} checked, {} alerts created, {} failures",
            checked, alertsCreated, failures);
        return new AnomalyRunSummary(checked, alertsCreated, failures, null);
    }

    List<PerformanceApi.DayEnergy> buildDays(Equipment equipment, LocalDate from, LocalDate to) {
        Site site = equipment.getSite();
        Map<LocalDate, Double> sunHours =
            siteForecastService.sunHoursByDate(site.getLatitude(), site.getLongitude());
        Map<LocalDate, Double> actual = actualEnergyByDate(equipment.getId(), from, to);

        double capacityKw = equipment.getCapacityKw().doubleValue();
        double performanceRatio = siteForecastService.performanceRatio().doubleValue();

        List<PerformanceApi.DayEnergy> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            Double sun = sunHours.get(date);
            Double expected = sun == null ? null : capacityKw * sun * performanceRatio;
            days.add(new PerformanceApi.DayEnergy(date.toString(), expected, actual.get(date)));
        }
        return days;
    }

    private Map<LocalDate, Double> actualEnergyByDate(Long equipmentId, LocalDate from, LocalDate to) {
        OffsetDateTime fromTs = from.atStartOfDay(SITE_ZONE).toOffsetDateTime();
        OffsetDateTime toTs = to.plusDays(1).atStartOfDay(SITE_ZONE).toOffsetDateTime();

        Map<LocalDate, Double> result = new HashMap<>();
        for (DailySummaryProjection day : sensorReadingRepository
            .findDailySummary(equipmentId, fromTs, toTs, SITE_ZONE.getId())) {
            // null = readings without energy values: unknown, so the day is left out
            if (day.getTotalEnergyKwh() != null) {
                result.put(LocalDate.parse(day.getDay()), day.getTotalEnergyKwh().doubleValue());
            }
        }
        return result;
    }

    private boolean raiseAlert(Long equipmentId, PerformanceApi.Finding finding) {
        return alertService.raiseIfNoActiveAlert(
            equipmentId,
            AlertType.valueOf(finding.type()),
            AlertSeverity.valueOf(finding.severity()),
            "Performance check " + finding.date() + ": " + finding.message());
    }
}

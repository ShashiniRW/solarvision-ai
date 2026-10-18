package com.solarvision.backend.monitoring;

import com.solarvision.backend.alert.AlertService;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates fake sensor readings for ACTIVE inverters on a schedule, runs each reading
 * through the alert engine, and occasionally injects a fault so alerts can be seen.
 * Only exists as a bean when app.simulator.enabled=true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class SensorReadingSimulator {

    // Sun is up between these local hours (simple model)
    private static final double SUNRISE_HOUR = 6.0;
    private static final double SUNSET_HOUR = 18.0;

    // For now every site uses one time zone; later this will come from the site
    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");

    private final EquipmentRepository equipmentRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AlertService alertService;
    private final Random random = new Random();

    @Value("${app.simulator.interval-ms:60000}")
    private long intervalMs;

    // Chance (0.0 to 1.0) that a generated reading contains a fault. 0 = never.
    @Value("${app.simulator.fault-probability:0.0}")
    private double faultProbability;

    @Scheduled(fixedRateString = "${app.simulator.interval-ms:60000}")
    public void generateReadings() {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        List<Equipment> inverters =
                equipmentRepository.findByEquipmentTypeAndStatus("INVERTER", "ACTIVE");

        List<SensorReading> readings = new ArrayList<>();
        for (Equipment inverter : inverters) {
            if (inverter.getCapacityKw() == null) {
                continue; // cannot simulate output without a capacity
            }
            readings.add(buildReading(inverter, now));
        }

        if (readings.isEmpty()) {
            return;
        }

        List<SensorReading> saved = sensorReadingRepository.saveAll(readings);
        log.info("Simulator saved {} reading(s) at {}", saved.size(), now);

        // Same path as real data: every reading goes through the alert rules
        for (SensorReading reading : saved) {
            try {
                alertService.evaluate(reading);
            } catch (RuntimeException ex) {
                log.error("Alert evaluation failed for reading {}", reading.getId(), ex);
            }
        }
    }

    private SensorReading buildReading(Equipment inverter, OffsetDateTime now) {
        ZonedDateTime local = now.atZoneSameInstant(SITE_ZONE);
        double hour = local.getHour() + local.getMinute() / 60.0;

        // Solar curve: 0 at night, smooth rise to a peak at midday, smooth fall
        double sun = 0.0;
        if (hour > SUNRISE_HOUR && hour < SUNSET_HOUR) {
            sun = Math.sin(Math.PI * (hour - SUNRISE_HOUR) / (SUNSET_HOUR - SUNRISE_HOUR));
        }

        // Random cloud effect between 70% and 100% of the clear-sky output
        double cloud = 0.7 + 0.3 * random.nextDouble();
        double load = sun * cloud;

        double powerKw = inverter.getCapacityKw().doubleValue() * load;
        double intervalHours = intervalMs / 3_600_000.0;
        double energyKwh = powerKw * intervalHours;            // energy produced since the last reading
        double voltageV = 230 + (random.nextDouble() - 0.5) * 6;
        double currentA = powerKw * 1000 / voltageV;
        double temperatureC = 25 + 20 * load + (random.nextDouble() - 0.5) * 2;

        SensorReading reading = new SensorReading();
        reading.setEquipment(inverter);
        reading.setRecordedAt(now);
        reading.setPowerKw(round(powerKw, 3));
        reading.setEnergyKwh(round(energyKwh, 3));
        reading.setVoltageV(round(voltageV, 2));
        reading.setCurrentA(round(currentA, 2));
        reading.setTemperatureC(round(temperatureC, 2));
        reading.setIsSimulated(true);

        maybeInjectFault(reading);
        return reading;
    }

    /** With probability faultProbability, makes the reading look like a broken inverter. */
    private void maybeInjectFault(SensorReading reading) {
        if (faultProbability <= 0 || random.nextDouble() >= faultProbability) {
            return;
        }

        switch (random.nextInt(3)) {
            case 0 -> // overheating: 70 to 90 C
                    reading.setTemperatureC(round(70 + random.nextDouble() * 20, 2));
            case 1 -> { // dead inverter: no output (only raises an alert during the day)
                reading.setPowerKw(round(0, 3));
                reading.setEnergyKwh(round(0, 3));
                reading.setCurrentA(round(0, 2));
            }
            default -> // grid voltage problem: sag (190-200 V) or surge (255-275 V)
                    reading.setVoltageV(round(random.nextBoolean()
                            ? 190 + random.nextDouble() * 10
                            : 255 + random.nextDouble() * 20, 2));
        }
    }

    private BigDecimal round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
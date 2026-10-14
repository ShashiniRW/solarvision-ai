package com.solarvision.backend.monitoring;

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
 * Generates fake sensor readings for ACTIVE inverters on a schedule.
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
    private final Random random = new Random();

    @Value("${app.simulator.interval-ms:60000}")
    private long intervalMs;

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

        if (!readings.isEmpty()) {
            sensorReadingRepository.saveAll(readings);
            log.info("Simulator saved {} reading(s) at {}", readings.size(), now);
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
        return reading;
    }

    private BigDecimal round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
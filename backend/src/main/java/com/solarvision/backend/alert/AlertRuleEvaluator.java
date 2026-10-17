package com.solarvision.backend.alert;

import com.solarvision.backend.monitoring.SensorReading;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure rule logic: looks at ONE sensor reading and says which rules it breaks.
 * No database access, so it is easy to unit-test.
 */
@Component
public class AlertRuleEvaluator {

    // Temperature limits (degrees C)
    private static final BigDecimal TEMP_WARNING = new BigDecimal("65");
    private static final BigDecimal TEMP_CRITICAL = new BigDecimal("75");

    // Voltage limits (volts): nominal 230 V +/- 10% is a warning, far outside is critical
    private static final BigDecimal VOLT_WARNING_LOW = new BigDecimal("207");
    private static final BigDecimal VOLT_WARNING_HIGH = new BigDecimal("253");
    private static final BigDecimal VOLT_CRITICAL_LOW = new BigDecimal("180");
    private static final BigDecimal VOLT_CRITICAL_HIGH = new BigDecimal("280");

    // Zero power only matters while the sun is clearly up (local time)
    private static final BigDecimal ZERO_POWER_LIMIT_KW = new BigDecimal("0.01");
    private static final int DAY_START_HOUR = 8;   // inclusive
    private static final int DAY_END_HOUR = 16;    // exclusive

    // For now every site uses one time zone; later this will come from the site
    private static final ZoneId SITE_ZONE = ZoneId.of("Asia/Colombo");

    public List<AlertCandidate> evaluate(SensorReading reading) {
        List<AlertCandidate> candidates = new ArrayList<>();
        checkTemperature(reading).ifPresent(candidates::add);
        checkZeroPower(reading).ifPresent(candidates::add);
        checkVoltage(reading).ifPresent(candidates::add);
        return candidates;
    }

    private Optional<AlertCandidate> checkTemperature(SensorReading reading) {
        BigDecimal temperature = reading.getTemperatureC();
        if (temperature == null) {
            return Optional.empty();
        }
        if (temperature.compareTo(TEMP_CRITICAL) > 0) {
            return Optional.of(new AlertCandidate(AlertType.OVER_TEMPERATURE, AlertSeverity.CRITICAL,
                    "Temperature " + temperature + " C exceeds the critical limit of " + TEMP_CRITICAL + " C"));
        }
        if (temperature.compareTo(TEMP_WARNING) > 0) {
            return Optional.of(new AlertCandidate(AlertType.OVER_TEMPERATURE, AlertSeverity.WARNING,
                    "Temperature " + temperature + " C exceeds the warning limit of " + TEMP_WARNING + " C"));
        }
        return Optional.empty();
    }

    private Optional<AlertCandidate> checkZeroPower(SensorReading reading) {
        BigDecimal power = reading.getPowerKw();
        if (power == null || reading.getRecordedAt() == null) {
            return Optional.empty();
        }
        int localHour = reading.getRecordedAt().atZoneSameInstant(SITE_ZONE).getHour();
        boolean daytime = localHour >= DAY_START_HOUR && localHour < DAY_END_HOUR;

        if (daytime && power.compareTo(ZERO_POWER_LIMIT_KW) <= 0) {
            return Optional.of(new AlertCandidate(AlertType.ZERO_POWER, AlertSeverity.CRITICAL,
                    "No power output (" + power + " kW) during daytime"));
        }
        return Optional.empty();
    }

    private Optional<AlertCandidate> checkVoltage(SensorReading reading) {
        BigDecimal voltage = reading.getVoltageV();
        if (voltage == null) {
            return Optional.empty();
        }
        if (voltage.compareTo(VOLT_CRITICAL_LOW) < 0 || voltage.compareTo(VOLT_CRITICAL_HIGH) > 0) {
            return Optional.of(new AlertCandidate(AlertType.VOLTAGE_OUT_OF_RANGE, AlertSeverity.CRITICAL,
                    "Voltage " + voltage + " V is far outside the safe range of "
                            + VOLT_CRITICAL_LOW + " to " + VOLT_CRITICAL_HIGH + " V"));
        }
        if (voltage.compareTo(VOLT_WARNING_LOW) < 0 || voltage.compareTo(VOLT_WARNING_HIGH) > 0) {
            return Optional.of(new AlertCandidate(AlertType.VOLTAGE_OUT_OF_RANGE, AlertSeverity.WARNING,
                    "Voltage " + voltage + " V is outside the normal range of "
                            + VOLT_WARNING_LOW + " to " + VOLT_WARNING_HIGH + " V"));
        }
        return Optional.empty();
    }
}
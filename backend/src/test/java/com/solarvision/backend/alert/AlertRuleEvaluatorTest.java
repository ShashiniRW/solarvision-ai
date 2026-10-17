package com.solarvision.backend.alert;

import com.solarvision.backend.monitoring.SensorReading;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRuleEvaluatorTest {

    private final AlertRuleEvaluator evaluator = new AlertRuleEvaluator();

    // ---------- helpers ----------

    // Time is written in the site's local offset (+05:30). A healthy reading by default.
    private SensorReading reading(String time, String power, String voltage, String temperature) {
        SensorReading reading = new SensorReading();
        reading.setRecordedAt(OffsetDateTime.parse(time));
        reading.setPowerKw(power == null ? null : new BigDecimal(power));
        reading.setVoltageV(voltage == null ? null : new BigDecimal(voltage));
        reading.setTemperatureC(temperature == null ? null : new BigDecimal(temperature));
        return reading;
    }

    private SensorReading healthyAt(String time) {
        return reading(time, "4.5", "230", "40");
    }

    // ---------- healthy ----------

    @Test
    void healthyReading_raisesNoAlerts() {
        assertThat(evaluator.evaluate(healthyAt("2026-10-09T12:00:00+05:30"))).isEmpty();
    }

    // ---------- temperature ----------

    @Test
    void temperature_exactlyAtWarningLimit_raisesNothing() {
        assertThat(evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "230", "65"))).isEmpty();
    }

    @Test
    void temperature_justAboveWarningLimit_raisesWarning() {
        List<AlertCandidate> result =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "230", "65.01"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(AlertType.OVER_TEMPERATURE);
        assertThat(result.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void temperature_aboveCriticalLimit_raisesSingleCriticalAlert() {
        List<AlertCandidate> result =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "230", "80"));

        assertThat(result).hasSize(1); // one alert, not warning AND critical
        assertThat(result.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    // ---------- zero power ----------

    @Test
    void zeroPower_atMidday_raisesCritical() {
        List<AlertCandidate> result =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "0", "230", "40"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(AlertType.ZERO_POWER);
        assertThat(result.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void zeroPower_atNight_raisesNothing() {
        assertThat(evaluator.evaluate(reading("2026-10-09T22:00:00+05:30", "0", "230", "30"))).isEmpty();
    }

    @Test
    void zeroPower_justBeforeDayWindow_raisesNothing() {
        assertThat(evaluator.evaluate(reading("2026-10-09T07:59:00+05:30", "0", "230", "30"))).isEmpty();
    }

    @Test
    void zeroPower_atStartOfDayWindow_raisesAlert() {
        assertThat(evaluator.evaluate(reading("2026-10-09T08:00:00+05:30", "0", "230", "30")))
                .hasSize(1);
    }

    @Test
    void zeroPower_atEndOfDayWindow_raisesNothing() {
        assertThat(evaluator.evaluate(reading("2026-10-09T16:00:00+05:30", "0", "230", "30"))).isEmpty();
    }

    @Test
    void missingPower_raisesNothing() {
        assertThat(evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", null, "230", "40"))).isEmpty();
    }

    // ---------- voltage ----------

    @Test
    void voltage_exactlyAtLowerWarningLimit_raisesNothing() {
        assertThat(evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "207", "40"))).isEmpty();
    }

    @Test
    void voltage_justBelowLowerWarningLimit_raisesWarning() {
        List<AlertCandidate> result =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "206.9", "40"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(AlertType.VOLTAGE_OUT_OF_RANGE);
        assertThat(result.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void voltage_justAboveUpperWarningLimit_raisesWarning() {
        List<AlertCandidate> result =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "253.1", "40"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).severity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void voltage_farOutsideRange_raisesCritical() {
        List<AlertCandidate> low =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "179.9", "40"));
        List<AlertCandidate> high =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "4.5", "280.1", "40"));

        assertThat(low.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(high.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    // ---------- several problems at once ----------

    @Test
    void severalProblems_raiseOneAlertPerRule() {
        List<AlertCandidate> result =
                evaluator.evaluate(reading("2026-10-09T12:00:00+05:30", "0", "150", "90"));

        assertThat(result).extracting(AlertCandidate::type)
                .containsExactlyInAnyOrder(
                        AlertType.OVER_TEMPERATURE,
                        AlertType.ZERO_POWER,
                        AlertType.VOLTAGE_OUT_OF_RANGE);
    }
}
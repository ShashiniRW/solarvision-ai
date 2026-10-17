package com.solarvision.backend.alert;

import com.solarvision.backend.monitoring.SensorReading;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleEvaluator ruleEvaluator;
    private final AlertRepository alertRepository;

    /**
     * Runs the alert rules against a saved reading and raises an alert for every
     * broken rule that does not already have an unresolved alert.
     *
     * @return how many NEW alerts were created
     */
    @Transactional
    public int evaluate(SensorReading reading) {
        int created = 0;

        for (AlertCandidate candidate : ruleEvaluator.evaluate(reading)) {
            int inserted = alertRepository.insertIfNoActiveAlert(
                    reading.getEquipment().getId(),
                    reading.getId(),
                    candidate.type().name(),
                    candidate.severity().name(),
                    candidate.message(),
                    reading.getRecordedAt());

            if (inserted > 0) {
                created++;
                log.warn("Alert raised: {} {} on equipment {} - {}",
                        candidate.severity(), candidate.type(),
                        reading.getEquipment().getId(), candidate.message());
            }
        }
        return created;
    }
}
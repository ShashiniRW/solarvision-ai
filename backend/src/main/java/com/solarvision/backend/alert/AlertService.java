package com.solarvision.backend.alert;

import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.monitoring.SensorReading;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int MAX_PAGE_SIZE = 100;

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

    @Transactional(readOnly = true)
    public List<Alert> search(AlertStatus status, Long equipmentId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        if (status != null && equipmentId != null) {
            return alertRepository.findByStatusAndEquipmentIdOrderByTriggeredAtDesc(status, equipmentId, pageable);
        }
        if (status != null) {
            return alertRepository.findByStatusOrderByTriggeredAtDesc(status, pageable);
        }
        if (equipmentId != null) {
            return alertRepository.findByEquipmentIdOrderByTriggeredAtDesc(equipmentId, pageable);
        }
        return alertRepository.findAllByOrderByTriggeredAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Alert getById(Long id) {
        return alertRepository.findWithEquipmentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));
    }
}
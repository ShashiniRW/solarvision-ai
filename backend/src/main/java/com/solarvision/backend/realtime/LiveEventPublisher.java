package com.solarvision.backend.realtime;

import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.equipment.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Services call this when something changes. It only ANNOUNCES the change;
 * LiveEventRegistry sends it to browsers after the database transaction commits.
 */
@Component
@RequiredArgsConstructor
public class LiveEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final EquipmentRepository equipmentRepository;

    public void alertCreated(Long equipmentId, AlertSeverity severity, String message) {
        equipmentRepository.findProjectIdForEquipment(equipmentId)
            .ifPresent(projectId -> eventPublisher.publishEvent(
                new LiveEvent(LiveEventType.ALERT_CREATED, projectId, severity.name(), message)));
    }

    public void alertUpdated(Long projectId) {
        eventPublisher.publishEvent(new LiveEvent(LiveEventType.ALERT_UPDATED, projectId, null, null));
    }

    /** One event per project, even when many readings were saved at once. */
    public void readingsRecorded(Collection<Long> equipmentIds) {
        if (equipmentIds.isEmpty()) {
            return;
        }
        for (Long projectId : equipmentRepository.findProjectIdsForEquipment(equipmentIds)) {
            eventPublisher.publishEvent(new LiveEvent(LiveEventType.READINGS_RECORDED, projectId, null, null));
        }
    }
}

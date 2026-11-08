package com.solarvision.backend.alert;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.access.ProjectScope;
import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.monitoring.SensorReading;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AlertRuleEvaluator ruleEvaluator;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;

    /**
     * Runs the alert rules against a saved reading and raises an alert for every
     * broken rule that does not already have an unresolved alert.
     * NOT scoped: called during ingestion, where nobody is logged in.
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

        // 1. one equipment: check it first (404 if hidden), then the existing queries are safe
        if (equipmentId != null) {
            accessService.requireEquipmentAccess(equipmentId);
            return status != null
                    ? alertRepository.findByStatusAndEquipmentIdOrderByTriggeredAtDesc(status, equipmentId, pageable)
                    : alertRepository.findByEquipmentIdOrderByTriggeredAtDesc(equipmentId, pageable);
        }

        ProjectScope scope = accessService.currentScope();

        // 2. ADMIN: everything
        if (scope.unrestricted()) {
            return status != null
                    ? alertRepository.findByStatusOrderByTriggeredAtDesc(status, pageable)
                    : alertRepository.findAllByOrderByTriggeredAtDesc(pageable);
        }

        // 3. a user with no projects sees nothing (no database call needed)
        if (scope.projectIds().isEmpty()) {
            return List.of();
        }

        // 4. everyone else: the database filters by the user's projects
        return status != null
                ? alertRepository.findByStatusInProjects(status, scope.projectIds(), pageable)
                : alertRepository.findInProjects(scope.projectIds(), pageable);
    }

    @Transactional(readOnly = true)
    public Alert getById(Long id) {
        return findVisibleAlert(id);
    }

    /** OPEN -> ACKNOWLEDGED */
    @Transactional
    public Alert acknowledge(Long alertId, String userEmail) {
        Alert alert = findVisibleAlert(alertId);

        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new ConflictException(
                    "Only an OPEN alert can be acknowledged (current status: " + alert.getStatus() + ")");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert.setAcknowledgedBy(userIdFor(userEmail));
        return alertRepository.save(alert);
    }

    /** OPEN or ACKNOWLEDGED -> RESOLVED */
    @Transactional
    public Alert resolve(Long alertId, String userEmail) {
        Alert alert = findVisibleAlert(alertId);

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ConflictException("Alert is already resolved");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(OffsetDateTime.now());
        alert.setResolvedBy(userIdFor(userEmail));
        return alertRepository.save(alert);
    }

    /**
     * Loads an alert, or throws 404 when it does not exist OR belongs to a project
     * the current user may not see (same message, so the two cases look identical).
     */
    private Alert findVisibleAlert(Long id) {
        Alert alert = alertRepository.findWithEquipmentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));

        // we are inside a transaction, so reading site -> project here is safe
        Long projectId = alert.getEquipment().getSite().getProject().getId();
        if (!accessService.canAccessProject(projectId)) {
            throw new ResourceNotFoundException("Alert not found with id: " + id);
        }
        return alert;
    }

    private Long userIdFor(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
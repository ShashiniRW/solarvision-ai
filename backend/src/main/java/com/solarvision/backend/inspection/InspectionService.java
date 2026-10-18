package com.solarvision.backend.inspection;

import com.solarvision.backend.alert.Alert;
import com.solarvision.backend.alert.AlertRepository;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.equipment.EquipmentRepository;
import com.solarvision.backend.inspection.dto.CompleteInspectionRequest;
import com.solarvision.backend.inspection.dto.CreateInspectionRequest;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    @Transactional
    public Inspection create(CreateInspectionRequest request, String creatorEmail) {

        if (request.getInspectionType() == InspectionType.ALERT_FOLLOW_UP && request.getAlertId() == null) {
            throw new IllegalArgumentException("An alert follow-up inspection must reference an alert");
        }

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Equipment not found with id: " + request.getEquipmentId()));

        if (request.getAlertId() != null) {
            Alert alert = alertRepository.findWithEquipmentById(request.getAlertId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Alert not found with id: " + request.getAlertId()));
            if (!alert.getEquipment().getId().equals(equipment.getId())) {
                throw new IllegalArgumentException("The alert does not belong to this equipment");
            }
        }

        if (request.getAssignedTo() != null && !userRepository.existsById(request.getAssignedTo())) {
            throw new ResourceNotFoundException("User not found with id: " + request.getAssignedTo());
        }

        Inspection inspection = new Inspection();
        inspection.setEquipment(equipment);
        inspection.setAlertId(request.getAlertId());
        inspection.setInspectionType(request.getInspectionType());
        inspection.setStatus(InspectionStatus.SCHEDULED);
        inspection.setScheduledAt(request.getScheduledAt());
        inspection.setAssignedTo(request.getAssignedTo());
        inspection.setCreatedBy(userIdFor(creatorEmail));
        return inspectionRepository.save(inspection);
    }

    @Transactional(readOnly = true)
    public Inspection getById(Long id) {
        return inspectionRepository.findWithEquipmentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found with id: " + id));
    }

    /** SCHEDULED -> IN_PROGRESS */
    @Transactional
    public Inspection start(Long id, String userEmail) {
        Inspection inspection = getById(id);

        if (inspection.getStatus() != InspectionStatus.SCHEDULED) {
            throw new IllegalArgumentException(
                    "Only a SCHEDULED inspection can be started (current status: " + inspection.getStatus() + ")");
        }

        Long userId = userIdFor(userEmail);
        inspection.setStatus(InspectionStatus.IN_PROGRESS);
        inspection.setStartedAt(OffsetDateTime.now());
        if (inspection.getAssignedTo() == null) {
            inspection.setAssignedTo(userId); // whoever picks it up owns it
        }
        return inspectionRepository.save(inspection);
    }

    /** IN_PROGRESS -> COMPLETED */
    @Transactional
    public Inspection complete(Long id, CompleteInspectionRequest request) {
        Inspection inspection = getById(id);

        if (inspection.getStatus() != InspectionStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Only an IN_PROGRESS inspection can be completed (current status: " + inspection.getStatus() + ")");
        }

        String findings = request.getFindings() == null ? "" : request.getFindings().trim();
        if (request.getResult() != InspectionResult.PASSED && findings.isEmpty()) {
            throw new IllegalArgumentException("Findings are required unless the inspection passed");
        }

        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setResult(request.getResult());
        inspection.setFindings(findings.isEmpty() ? null : findings);
        inspection.setCompletedAt(OffsetDateTime.now());
        return inspectionRepository.save(inspection);
    }

    /** SCHEDULED or IN_PROGRESS -> CANCELLED */
    @Transactional
    public Inspection cancel(Long id, String reason) {
        Inspection inspection = getById(id);

        if (inspection.getStatus() == InspectionStatus.COMPLETED
                || inspection.getStatus() == InspectionStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "A " + inspection.getStatus() + " inspection cannot be cancelled");
        }

        inspection.setStatus(InspectionStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            inspection.setFindings(reason.trim());
        }
        return inspectionRepository.save(inspection);
    }

    private Long userIdFor(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
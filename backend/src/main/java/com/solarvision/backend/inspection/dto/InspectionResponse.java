package com.solarvision.backend.inspection.dto;

import com.solarvision.backend.equipment.Equipment;
import com.solarvision.backend.inspection.Inspection;
import com.solarvision.backend.inspection.InspectionResult;
import com.solarvision.backend.inspection.InspectionStatus;
import com.solarvision.backend.inspection.InspectionType;

import java.time.OffsetDateTime;

public record InspectionResponse(
        Long id,
        Long equipmentId,
        String equipmentType,
        String equipmentSerialNumber,
        Long alertId,
        InspectionType inspectionType,
        InspectionStatus status,
        OffsetDateTime scheduledAt,
        Long assignedTo,
        Long createdBy,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        InspectionResult result,
        String findings
) {
    public static InspectionResponse fromEntity(Inspection inspection) {
        Equipment equipment = inspection.getEquipment();
        return new InspectionResponse(
                inspection.getId(),
                equipment.getId(),
                equipment.getEquipmentType(),
                equipment.getSerialNumber(),
                inspection.getAlertId(),
                inspection.getInspectionType(),
                inspection.getStatus(),
                inspection.getScheduledAt(),
                inspection.getAssignedTo(),
                inspection.getCreatedBy(),
                inspection.getStartedAt(),
                inspection.getCompletedAt(),
                inspection.getResult(),
                inspection.getFindings()
        );
    }
}
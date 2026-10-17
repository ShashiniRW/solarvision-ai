package com.solarvision.backend.alert.dto;

import com.solarvision.backend.alert.Alert;
import com.solarvision.backend.alert.AlertSeverity;
import com.solarvision.backend.alert.AlertStatus;
import com.solarvision.backend.alert.AlertType;
import com.solarvision.backend.equipment.Equipment;

import java.time.OffsetDateTime;

public record AlertResponse(
        Long id,
        Long equipmentId,
        String equipmentType,
        String equipmentSerialNumber,
        AlertType alertType,
        AlertSeverity severity,
        AlertStatus status,
        String message,
        OffsetDateTime triggeredAt,
        OffsetDateTime acknowledgedAt,
        Long acknowledgedBy,
        OffsetDateTime resolvedAt,
        Long resolvedBy,
        Long readingId
) {
    public static AlertResponse fromEntity(Alert alert) {
        Equipment equipment = alert.getEquipment();
        return new AlertResponse(
                alert.getId(),
                equipment.getId(),
                equipment.getEquipmentType(),
                equipment.getSerialNumber(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getMessage(),
                alert.getTriggeredAt(),
                alert.getAcknowledgedAt(),
                alert.getAcknowledgedBy(),
                alert.getResolvedAt(),
                alert.getResolvedBy(),
                alert.getReadingId()
        );
    }
}
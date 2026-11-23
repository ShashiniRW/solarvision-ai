package com.solarvision.backend.alert;

/** How many unresolved alerts of one severity a piece of equipment has. */
public record EquipmentAlertCount(Long equipmentId, AlertSeverity severity, Long count) {
}

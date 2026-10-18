package com.solarvision.backend.inspection.dto;

import com.solarvision.backend.inspection.InspectionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CreateInspectionRequest {

    @NotNull(message = "equipmentId is required")
    private Long equipmentId;

    @NotNull(message = "inspectionType is required")
    private InspectionType inspectionType;

    @NotNull(message = "scheduledAt is required")
    private OffsetDateTime scheduledAt;

    // optional: the alert this inspection follows up on
    private Long alertId;

    // optional: the technician who should do it
    private Long assignedTo;
}
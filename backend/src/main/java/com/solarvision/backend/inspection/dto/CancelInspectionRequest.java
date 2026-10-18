package com.solarvision.backend.inspection.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelInspectionRequest {

    @Size(max = 1000, message = "reason cannot exceed 1000 characters")
    private String reason;
}
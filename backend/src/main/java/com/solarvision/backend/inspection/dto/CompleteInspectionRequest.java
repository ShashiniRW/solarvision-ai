package com.solarvision.backend.inspection.dto;

import com.solarvision.backend.inspection.InspectionResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteInspectionRequest {

    @NotNull(message = "result is required")
    private InspectionResult result;

    // required by the service unless the result is PASSED
    @Size(max = 5000, message = "findings cannot exceed 5000 characters")
    private String findings;
}
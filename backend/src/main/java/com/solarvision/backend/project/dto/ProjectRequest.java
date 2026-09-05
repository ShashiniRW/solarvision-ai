package com.solarvision.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProjectRequest {

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    private Long customerId;

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;
}
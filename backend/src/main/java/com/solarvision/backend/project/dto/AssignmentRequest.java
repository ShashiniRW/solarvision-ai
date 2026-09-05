package com.solarvision.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Role in project is required")
    private String roleInProject;
}
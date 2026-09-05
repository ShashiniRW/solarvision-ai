package com.solarvision.backend.project.dto;

import com.solarvision.backend.project.ProjectAssignment;
import com.solarvision.backend.user.dto.UserSummaryResponse;

import java.time.LocalDateTime;

public record AssignmentResponse(
        Long id,
        Long projectId,
        String projectName,
        UserSummaryResponse user,
        String roleInProject,
        LocalDateTime assignedAt
) {
    public static AssignmentResponse fromEntity(ProjectAssignment assignment) {
        UserSummaryResponse userSummary = new UserSummaryResponse(
                assignment.getUser().getId(),
                assignment.getUser().getFirstName(),
                assignment.getUser().getLastName(),
                assignment.getUser().getEmail(),
                assignment.getUser().getStatus()
        );

        return new AssignmentResponse(
                assignment.getId(),
                assignment.getProject().getId(),
                assignment.getProject().getName(),
                userSummary,
                assignment.getRoleInProject(),
                assignment.getAssignedAt()
        );
    }
}
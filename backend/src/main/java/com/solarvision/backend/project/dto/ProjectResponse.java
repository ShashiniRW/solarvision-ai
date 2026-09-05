package com.solarvision.backend.project.dto;

import com.solarvision.backend.project.Project;
import com.solarvision.backend.user.dto.UserSummaryResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        Long organizationId,
        String organizationName,
        UserSummaryResponse customer,
        String name,
        String description,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProjectResponse fromEntity(Project project) {
        UserSummaryResponse customerSummary = null;
        if (project.getCustomer() != null) {
            customerSummary = new UserSummaryResponse(
                    project.getCustomer().getId(),
                    project.getCustomer().getFirstName(),
                    project.getCustomer().getLastName(),
                    project.getCustomer().getEmail(),
                    project.getCustomer().getStatus()
            );
        }

        return new ProjectResponse(
                project.getId(),
                project.getOrganization().getId(),
                project.getOrganization().getName(),
                customerSummary,
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
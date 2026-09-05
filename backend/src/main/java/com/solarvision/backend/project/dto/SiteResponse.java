package com.solarvision.backend.project.dto;

import com.solarvision.backend.project.Site;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SiteResponse(
        Long id,
        Long projectId,
        String projectName,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal capacityKw,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SiteResponse fromEntity(Site site) {
        return new SiteResponse(
                site.getId(),
                site.getProject().getId(),
                site.getProject().getName(),
                site.getName(),
                site.getAddress(),
                site.getLatitude(),
                site.getLongitude(),
                site.getCapacityKw(),
                site.getStatus(),
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
}
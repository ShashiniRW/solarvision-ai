package com.solarvision.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SiteRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "Site name is required")
    private String name;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal capacityKw;
}
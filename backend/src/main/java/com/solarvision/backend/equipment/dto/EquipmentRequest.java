package com.solarvision.backend.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class EquipmentRequest {

    @NotNull(message = "Site ID is required")
    private Long siteId;

    @NotBlank(message = "Equipment type is required")
    private String equipmentType;

    private String manufacturer;

    private String model;

    private String serialNumber;

    @NotBlank(message = "QR code is required")
    private String qrCode;

    private BigDecimal capacityKw;

    private LocalDate installationDate;

    private LocalDate warrantyExpiryDate;

    private Integer warrantyYears;
}
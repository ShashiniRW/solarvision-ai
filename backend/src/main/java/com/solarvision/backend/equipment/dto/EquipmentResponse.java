package com.solarvision.backend.equipment.dto;

import com.solarvision.backend.equipment.Equipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EquipmentResponse(
        Long id,
        Long siteId,
        String siteName,
        String equipmentType,
        String manufacturer,
        String model,
        String serialNumber,
        String qrCode,
        BigDecimal capacityKw,
        LocalDate installationDate,
        LocalDate warrantyExpiryDate,
        Integer warrantyYears,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EquipmentResponse fromEntity(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getSite().getId(),
                equipment.getSite().getName(),
                equipment.getEquipmentType(),
                equipment.getManufacturer(),
                equipment.getModel(),
                equipment.getSerialNumber(),
                equipment.getQrCode(),
                equipment.getCapacityKw(),
                equipment.getInstallationDate(),
                equipment.getWarrantyExpiryDate(),
                equipment.getWarrantyYears(),
                equipment.getStatus(),
                equipment.getCreatedAt(),
                equipment.getUpdatedAt()
        );
    }
}
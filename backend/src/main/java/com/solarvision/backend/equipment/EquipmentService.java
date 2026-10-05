package com.solarvision.backend.equipment;

import com.solarvision.backend.equipment.dto.EquipmentRequest;
import com.solarvision.backend.project.Site;
import com.solarvision.backend.project.SiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class EquipmentService {

    private static final Set<String> VALID_TYPES = Set.of("PANEL", "INVERTER", "BATTERY", "SENSOR", "METER");

    private final EquipmentRepository equipmentRepository;
    private final SiteRepository siteRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, SiteRepository siteRepository) {
        this.equipmentRepository = equipmentRepository;
        this.siteRepository = siteRepository;
    }

    public Equipment createEquipment(EquipmentRequest request) {

        if (!VALID_TYPES.contains(request.getEquipmentType())) {
            throw new IllegalArgumentException(
                    "Invalid equipment type. Must be one of: " + VALID_TYPES);
        }

        if (equipmentRepository.existsByQrCode(request.getQrCode())) {
            throw new IllegalArgumentException("QR code is already in use");
        }

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new IllegalArgumentException("Site not found"));

        Equipment equipment = new Equipment();
        equipment.setSite(site);
        equipment.setEquipmentType(request.getEquipmentType());
        equipment.setManufacturer(request.getManufacturer());
        equipment.setModel(request.getModel());
        equipment.setSerialNumber(request.getSerialNumber());
        equipment.setQrCode(request.getQrCode());
        equipment.setCapacityKw(request.getCapacityKw());
        equipment.setInstallationDate(request.getInstallationDate());
        equipment.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
        equipment.setWarrantyYears(request.getWarrantyYears());
        equipment.setStatus("ACTIVE");

        Equipment saved = equipmentRepository.save(equipment);
        return equipmentRepository.findByIdWithDetails(saved.getId()).orElseThrow();
    }

    public List<Equipment> getEquipmentBySite(Long siteId) {
        return equipmentRepository.findBySiteIdWithDetails(siteId);
    }

    public Equipment getEquipmentById(Long id) {
        return equipmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found"));
    }

    public Equipment getEquipmentByQrCode(String qrCode) {
        return equipmentRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found for this QR code"));
    }
}
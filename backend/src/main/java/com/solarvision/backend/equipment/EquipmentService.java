package com.solarvision.backend.equipment;

import com.solarvision.backend.access.AccessService;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
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
    private final AccessService accessService;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            SiteRepository siteRepository,
                            AccessService accessService) {
        this.equipmentRepository = equipmentRepository;
        this.siteRepository = siteRepository;
        this.accessService = accessService;
    }

    public Equipment createEquipment(EquipmentRequest request) {

        if (!VALID_TYPES.contains(request.getEquipmentType())) {
            throw new IllegalArgumentException(
                    "Invalid equipment type. Must be one of: " + VALID_TYPES);
        }

        // access check first, so a stranger cannot probe which QR codes exist
        Site site = findAccessibleSite(request.getSiteId());

        if (equipmentRepository.existsByQrCode(request.getQrCode())) {
            throw new IllegalArgumentException("QR code is already in use");
        }

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
        findAccessibleSite(siteId);
        return equipmentRepository.findBySiteIdWithDetails(siteId);
    }

    public Equipment getEquipmentById(Long id) {
        Equipment equipment = equipmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found"));
        requireVisible(equipment, "Equipment not found");
        return equipment;
    }

    public Equipment getEquipmentByQrCode(String qrCode) {
        String message = "Equipment not found for this QR code";

        Equipment found = equipmentRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException(message));

        // reload with site + project fetched, so reading the project id is safe
        Equipment equipment = equipmentRepository.findByIdWithDetails(found.getId())
                .orElseThrow(() -> new ResourceNotFoundException(message));

        requireVisible(equipment, message);
        return equipment;
    }

    /** A site the user may not see looks exactly like a site that does not exist. */
    private Site findAccessibleSite(Long siteId) {
        Site site = siteRepository.findByIdWithDetails(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));

        if (!accessService.canAccessProject(site.getProject().getId())) {
            throw new ResourceNotFoundException("Site not found");
        }
        return site;
    }

    private void requireVisible(Equipment equipment, String notFoundMessage) {
        Long projectId = equipment.getSite().getProject().getId();
        if (!accessService.canAccessProject(projectId)) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
    }
}
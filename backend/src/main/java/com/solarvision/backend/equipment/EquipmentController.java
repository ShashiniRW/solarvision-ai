package com.solarvision.backend.equipment;

import com.solarvision.backend.equipment.dto.EquipmentRequest;
import com.solarvision.backend.equipment.dto.EquipmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        Equipment savedEquipment = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EquipmentResponse.fromEntity(savedEquipment));
    }

    @GetMapping
    public ResponseEntity<List<EquipmentResponse>> getEquipmentBySite(@RequestParam Long siteId) {
        List<EquipmentResponse> responses = equipmentService.getEquipmentBySite(siteId).stream()
                .map(EquipmentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getEquipmentById(@PathVariable Long id) {
        Equipment equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(EquipmentResponse.fromEntity(equipment));
    }

    @GetMapping("/scan/{qrCode}")
    public ResponseEntity<EquipmentResponse> getEquipmentByQrCode(@PathVariable String qrCode) {
        Equipment equipment = equipmentService.getEquipmentByQrCode(qrCode);
        return ResponseEntity.ok(EquipmentResponse.fromEntity(equipment));
    }
}
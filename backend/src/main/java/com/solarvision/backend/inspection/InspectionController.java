package com.solarvision.backend.inspection;

import com.solarvision.backend.inspection.dto.CancelInspectionRequest;
import com.solarvision.backend.inspection.dto.CompleteInspectionRequest;
import com.solarvision.backend.inspection.dto.CreateInspectionRequest;
import com.solarvision.backend.inspection.dto.InspectionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
    @PostMapping
    public ResponseEntity<InspectionResponse> create(
            @Valid @RequestBody CreateInspectionRequest request,
            Authentication authentication) {

        Inspection created = inspectionService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(InspectionResponse.fromEntity(created));
    }

    @GetMapping
    public ResponseEntity<List<InspectionResponse>> list(
            @RequestParam(required = false) InspectionStatus status,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<InspectionResponse> inspections = inspectionService.search(status, equipmentId, page, size)
                .stream()
                .map(InspectionResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(inspections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(InspectionResponse.fromEntity(inspectionService.getById(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER', 'TECHNICIAN')")
    @PostMapping("/{id}/start")
    public ResponseEntity<InspectionResponse> start(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(
                InspectionResponse.fromEntity(inspectionService.start(id, authentication.getName())));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER', 'TECHNICIAN')")
    @PostMapping("/{id}/complete")
    public ResponseEntity<InspectionResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteInspectionRequest request) {

        return ResponseEntity.ok(InspectionResponse.fromEntity(inspectionService.complete(id, request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<InspectionResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelInspectionRequest request) {

        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(InspectionResponse.fromEntity(inspectionService.cancel(id, reason)));
    }
}
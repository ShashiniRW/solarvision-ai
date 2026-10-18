package com.solarvision.backend.alert;

import com.solarvision.backend.alert.dto.AlertResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> list(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<AlertResponse> alerts = alertService.search(status, equipmentId, page, size)
                .stream()
                .map(AlertResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(AlertResponse.fromEntity(alertService.getById(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER', 'TECHNICIAN')")
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(
                AlertResponse.fromEntity(alertService.acknowledge(id, authentication.getName())));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENGINEER', 'TECHNICIAN')")
    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertResponse> resolve(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(
                AlertResponse.fromEntity(alertService.resolve(id, authentication.getName())));
    }
}
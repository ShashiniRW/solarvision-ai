package com.solarvision.backend.project;

import com.solarvision.backend.project.dto.SiteRequest;
import com.solarvision.backend.project.dto.SiteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @PostMapping
    public ResponseEntity<SiteResponse> createSite(@Valid @RequestBody SiteRequest request) {
        Site savedSite = siteService.createSite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SiteResponse.fromEntity(savedSite));
    }

    @GetMapping
    public ResponseEntity<List<SiteResponse>> getSitesByProject(@RequestParam Long projectId) {
        List<SiteResponse> responses = siteService.getSitesByProject(projectId).stream()
                .map(SiteResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteResponse> getSiteById(@PathVariable Long id) {
        Site site = siteService.getSiteById(id);
        return ResponseEntity.ok(SiteResponse.fromEntity(site));
    }
}
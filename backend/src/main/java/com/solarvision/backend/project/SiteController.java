package com.solarvision.backend.project;

import com.solarvision.backend.project.dto.SiteRequest;
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
    public ResponseEntity<Site> createSite(@Valid @RequestBody SiteRequest request) {
        Site savedSite = siteService.createSite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSite);
    }

    @GetMapping
    public ResponseEntity<List<Site>> getSitesByProject(@RequestParam Long projectId) {
        return ResponseEntity.ok(siteService.getSitesByProject(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Site> getSiteById(@PathVariable Long id) {
        return ResponseEntity.ok(siteService.getSiteById(id));
    }
}
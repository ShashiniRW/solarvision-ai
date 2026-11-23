package com.solarvision.backend.twin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SiteTwinController {

    private final SiteTwinService siteTwinService;

    /** Any logged-in user who may see the site (404 otherwise). */
    @GetMapping("/api/sites/{siteId}/twin")
    public SiteTwinResponse getTwin(@PathVariable Long siteId) {
        return siteTwinService.getTwin(siteId);
    }
}

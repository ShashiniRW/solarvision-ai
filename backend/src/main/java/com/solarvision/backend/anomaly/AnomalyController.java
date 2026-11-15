package com.solarvision.backend.anomaly;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final PerformanceAnomalyService performanceAnomalyService;

    // runs the same check as the nightly job, immediately (testing and demos)
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public AnomalyRunSummary run() {
        return performanceAnomalyService.runDetection();
    }
}

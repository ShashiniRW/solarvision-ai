package com.solarvision.backend.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SitesTodayService sitesTodayService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    /** Energy today and live power per site, problem sites first. */
    @GetMapping("/sites-today")
    public ResponseEntity<List<SiteTodayItem>> getSitesToday() {
        return ResponseEntity.ok(sitesTodayService.getSitesToday());
    }
}

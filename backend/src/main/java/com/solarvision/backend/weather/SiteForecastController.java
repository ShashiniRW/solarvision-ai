package com.solarvision.backend.weather;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites")
public class SiteForecastController {

    private final SiteForecastService siteForecastService;

    public SiteForecastController(SiteForecastService siteForecastService) {
        this.siteForecastService = siteForecastService;
    }

    // 14 days: the last 7 days, today and the next 6 days
    @GetMapping("/{id}/forecast")
    public ResponseEntity<SiteForecastResponse> getForecast(@PathVariable Long id) {
        return ResponseEntity.ok(siteForecastService.getForecast(id));
    }
}

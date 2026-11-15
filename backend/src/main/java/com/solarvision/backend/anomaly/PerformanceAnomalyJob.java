package com.solarvision.backend.anomaly;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PerformanceAnomalyJob {

    private final PerformanceAnomalyService performanceAnomalyService;

    // every morning, once the previous day is complete (Colombo time)
    @Scheduled(cron = "${app.anomaly.cron:0 15 6 * * *}", zone = "Asia/Colombo")
    public void runDaily() {
        performanceAnomalyService.runDetection();
    }
}

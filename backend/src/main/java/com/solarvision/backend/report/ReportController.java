package com.solarvision.backend.report;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Reports for every signed-in user; the service limits them to projects the user may see. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** Example: GET /api/reports/monthly?projectId=3&month=2026-11 */
    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(@RequestParam Long projectId,
                                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return reportService.monthlyReport(projectId, month);
    }
}

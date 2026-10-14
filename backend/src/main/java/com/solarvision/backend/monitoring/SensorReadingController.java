package com.solarvision.backend.monitoring;

import com.solarvision.backend.monitoring.dto.CreateSensorReadingRequest;
import com.solarvision.backend.monitoring.dto.DailyEnergySummaryResponse;
import com.solarvision.backend.monitoring.dto.SensorReadingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/readings")
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    @PostMapping
    public ResponseEntity<SensorReadingResponse> create(
            @Valid @RequestBody CreateSensorReadingRequest request) {

        SensorReading saved = sensorReadingService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SensorReadingResponse.fromEntity(saved));
    }

    @GetMapping("/equipment/{equipmentId}/latest")
    public ResponseEntity<SensorReadingResponse> getLatest(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(
                SensorReadingResponse.fromEntity(sensorReadingService.getLatest(equipmentId)));
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<SensorReadingResponse>> getRange(
            @PathVariable Long equipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        List<SensorReadingResponse> readings = sensorReadingService
                .getRange(equipmentId, from, to)
                .stream()
                .map(SensorReadingResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(readings);
    }

    @GetMapping("/equipment/{equipmentId}/daily-summary")
    public ResponseEntity<List<DailyEnergySummaryResponse>> getDailySummary(
            @PathVariable Long equipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(sensorReadingService.getDailySummary(equipmentId, from, to));
    }
}
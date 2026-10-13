package com.solarvision.backend.monitoring;

import com.solarvision.backend.monitoring.dto.CreateSensorReadingRequest;
import com.solarvision.backend.monitoring.dto.SensorReadingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
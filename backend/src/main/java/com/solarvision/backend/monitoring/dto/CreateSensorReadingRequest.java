package com.solarvision.backend.monitoring.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class CreateSensorReadingRequest {

    @NotNull(message = "equipmentId is required")
    private Long equipmentId;

    // optional: if missing, the server uses the current time
    private OffsetDateTime recordedAt;

    @DecimalMin(value = "0.0", message = "powerKw cannot be negative")
    private BigDecimal powerKw;

    @DecimalMin(value = "0.0", message = "energyKwh cannot be negative")
    private BigDecimal energyKwh;

    private BigDecimal voltageV;
    private BigDecimal currentA;
    private BigDecimal temperatureC;

    // optional: if missing, treated as simulated (true)
    private Boolean isSimulated;
}
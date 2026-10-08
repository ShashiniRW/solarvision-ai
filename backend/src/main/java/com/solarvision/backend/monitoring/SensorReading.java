package com.solarvision.backend.monitoring;

import com.solarvision.backend.equipment.Equipment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sensor_readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "power_kw")
    private BigDecimal powerKw;

    @Column(name = "energy_kwh")
    private BigDecimal energyKwh;

    @Column(name = "voltage_v")
    private BigDecimal voltageV;

    @Column(name = "current_a")
    private BigDecimal currentA;

    @Column(name = "temperature_c")
    private BigDecimal temperatureC;

    @Column(name = "is_simulated", nullable = false)
    private Boolean isSimulated = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
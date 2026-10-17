package com.solarvision.backend.alert;

import com.solarvision.backend.equipment.Equipment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // the reading that triggered the alert (kept as a plain id)
    @Column(name = "reading_id")
    private Long readingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "triggered_at", nullable = false)
    private OffsetDateTime triggeredAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    // id of the user who acknowledged the alert
    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    // id of the user who resolved the alert
    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
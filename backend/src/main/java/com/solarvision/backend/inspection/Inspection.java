package com.solarvision.backend.inspection;

import com.solarvision.backend.equipment.Equipment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inspections")
@Getter
@Setter
@NoArgsConstructor
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // the alert this inspection follows up on, if any (kept as a plain id)
    @Column(name = "alert_id")
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_type", nullable = false, length = 20)
    private InspectionType inspectionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private InspectionStatus status = InspectionStatus.SCHEDULED;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    // id of the technician assigned to the inspection
    @Column(name = "assigned_to")
    private Long assignedTo;

    // id of the user who created the inspection
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InspectionResult result;

    @Column(columnDefinition = "TEXT")
    private String findings;

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
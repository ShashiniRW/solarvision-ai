package com.solarvision.backend.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // True if there is still an unresolved alert of this type for this equipment
    boolean existsByEquipmentIdAndAlertTypeAndStatusNot(Long equipmentId,
                                                        AlertType alertType,
                                                        AlertStatus status);

    /**
     * Inserts a new OPEN alert unless an unresolved alert of the same type already exists
     * for this equipment (enforced by the partial unique index uq_alerts_active).
     * Returns 1 if an alert was created, 0 if one was already active.
     */
    @Modifying
    @Query(value = """
            INSERT INTO alerts (equipment_id, reading_id, alert_type, severity, status,
                                message, triggered_at, created_at, updated_at)
            VALUES (:equipmentId, :readingId, :alertType, :severity, 'OPEN',
                    :message, :triggeredAt, NOW(), NOW())
            ON CONFLICT (equipment_id, alert_type) WHERE status <> 'RESOLVED'
            DO NOTHING
            """, nativeQuery = true)
    int insertIfNoActiveAlert(@Param("equipmentId") Long equipmentId,
                              @Param("readingId") Long readingId,
                              @Param("alertType") String alertType,
                              @Param("severity") String severity,
                              @Param("message") String message,
                              @Param("triggeredAt") OffsetDateTime triggeredAt);
}
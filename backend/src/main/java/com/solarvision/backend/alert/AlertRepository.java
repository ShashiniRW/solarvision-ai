package com.solarvision.backend.alert;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // True if there is still an unresolved alert of this type for this equipment
    boolean existsByEquipmentIdAndAlertTypeAndStatusNot(Long equipmentId,
                                                        AlertType alertType,
                                                        AlertStatus status);
}
package com.solarvision.backend.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    // Newest reading for one piece of equipment
    Optional<SensorReading> findFirstByEquipmentIdOrderByRecordedAtDesc(Long equipmentId);

    // All readings for one piece of equipment between two times, oldest first
    List<SensorReading> findByEquipmentIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long equipmentId, OffsetDateTime from, OffsetDateTime to);

    // Used to reject duplicate readings (same equipment, same moment)
    boolean existsByEquipmentIdAndRecordedAt(Long equipmentId, OffsetDateTime recordedAt);
}
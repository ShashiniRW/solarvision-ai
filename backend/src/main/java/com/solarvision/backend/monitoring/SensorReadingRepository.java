package com.solarvision.backend.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
            SELECT to_char(CAST(recorded_at AT TIME ZONE :zone AS date), 'YYYY-MM-DD') AS "day",
                   COALESCE(SUM(energy_kwh), 0)            AS "totalEnergyKwh",
                   MAX(power_kw)                           AS "peakPowerKw",
                   ROUND(AVG(temperature_c), 2)            AS "avgTemperatureC",
                   COUNT(*)                                AS "readingCount"
            FROM sensor_readings
            WHERE equipment_id = :equipmentId
              AND recorded_at >= :fromTs
              AND recorded_at <  :toTs
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<DailySummaryProjection> findDailySummary(@Param("equipmentId") Long equipmentId,
                                                  @Param("fromTs") OffsetDateTime fromTs,
                                                  @Param("toTs") OffsetDateTime toTs,
                                                  @Param("zone") String zone);
}
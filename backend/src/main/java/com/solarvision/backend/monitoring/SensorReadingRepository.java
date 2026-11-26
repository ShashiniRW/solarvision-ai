package com.solarvision.backend.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
                   SUM(energy_kwh)                         AS "totalEnergyKwh", -- NULL when no reading that day has an energy value (unknown, not 0)
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


    // total energy produced between two moments (null when there are no readings)
    @Query("SELECT SUM(r.energyKwh) FROM SensorReading r "
            + "WHERE r.recordedAt >= :rangeStart AND r.recordedAt < :rangeEnd")
    BigDecimal sumEnergyBetween(@Param("rangeStart") OffsetDateTime rangeStart,
                                @Param("rangeEnd") OffsetDateTime rangeEnd);

    // NEW (dashboard): same total, but only for equipment in the given projects
    @Query("SELECT SUM(r.energyKwh) FROM SensorReading r "
            + "WHERE r.recordedAt >= :rangeStart AND r.recordedAt < :rangeEnd "
            + "AND r.equipment.site.project.id IN :projectIds")
    BigDecimal sumEnergyBetweenInProjects(@Param("rangeStart") OffsetDateTime rangeStart,
                                          @Param("rangeEnd") OffsetDateTime rangeEnd,
                                          @Param("projectIds") Collection<Long> projectIds);

    /** The newest reading of every equipment at a site, in one query. */
    @Query("""
            select r from SensorReading r
            where r.equipment.site.id = :siteId
              and r.recordedAt = (select max(r2.recordedAt) from SensorReading r2
                                  where r2.equipment = r.equipment)
            """)
    List<SensorReading> findLatestForSite(@Param("siteId") Long siteId);

    /** Live power: the newest reading of every inverter, counted only if it arrived after 'since'. */
    @Query("""
            select sum(r.powerKw) from SensorReading r
            where r.equipment.equipmentType = 'INVERTER'
              and r.recordedAt >= :since
              and r.recordedAt = (select max(r2.recordedAt) from SensorReading r2
                                  where r2.equipment = r.equipment)
            """)
    BigDecimal sumLatestInverterPowerSince(@Param("since") OffsetDateTime since);

    /** Same as above, only for the given projects. */
    @Query("""
            select sum(r.powerKw) from SensorReading r
            where r.equipment.equipmentType = 'INVERTER'
              and r.equipment.site.project.id in :projectIds
              and r.recordedAt >= :since
              and r.recordedAt = (select max(r2.recordedAt) from SensorReading r2
                                  where r2.equipment = r.equipment)
            """)
    BigDecimal sumLatestInverterPowerSinceInProjects(@Param("since") OffsetDateTime since,
                                                     @Param("projectIds") Set<Long> projectIds);


    /** Energy between two moments, one total per site (only the given sites). */
    @Query("""
            select new com.solarvision.backend.monitoring.SiteValue(r.equipment.site.id, sum(r.energyKwh))
            from SensorReading r
            where r.recordedAt >= :rangeStart and r.recordedAt < :rangeEnd
              and r.equipment.site.id in :siteIds
            group by r.equipment.site.id
            """)
    List<SiteValue> sumEnergyBetweenBySite(@Param("rangeStart") OffsetDateTime rangeStart,
                                           @Param("rangeEnd") OffsetDateTime rangeEnd,
                                           @Param("siteIds") Collection<Long> siteIds);

    /** Live power per site: newest reading of every inverter, counted only if newer than 'since'. */
    @Query("""
            select new com.solarvision.backend.monitoring.SiteValue(r.equipment.site.id, sum(r.powerKw))
            from SensorReading r
            where r.equipment.equipmentType = 'INVERTER'
              and r.equipment.site.id in :siteIds
              and r.recordedAt >= :since
              and r.recordedAt = (select max(r2.recordedAt) from SensorReading r2
                                  where r2.equipment = r.equipment)
            group by r.equipment.site.id
            """)
    List<SiteValue> sumLatestInverterPowerSinceBySite(@Param("since") OffsetDateTime since,
                                                      @Param("siteIds") Collection<Long> siteIds);

}

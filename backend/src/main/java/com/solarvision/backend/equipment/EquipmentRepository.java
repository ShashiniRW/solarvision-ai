package com.solarvision.backend.equipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.site s " +
            "LEFT JOIN FETCH s.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "WHERE e.site.id = :siteId")
    List<Equipment> findBySiteIdWithDetails(Long siteId);

    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.site s " +
            "LEFT JOIN FETCH s.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "WHERE e.id = :id")
    Optional<Equipment> findByIdWithDetails(Long id);

    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.site s " +
            "LEFT JOIN FETCH s.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "WHERE e.qrCode = :qrCode")
    Optional<Equipment> findByQrCode(String qrCode);

    List<Equipment> findByEquipmentTypeAndStatus(String equipmentType, String status);

    long countByStatus(String status);
    boolean existsByQrCode(String qrCode);

    // only the project id of an equipment (no entity loading, no lazy-loading risk)
    @Query("SELECT e.site.project.id FROM Equipment e WHERE e.id = :id")
    Optional<Long> findProjectIdById(Long id);

    // NEW (dashboard): counts limited to the given projects
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.site.project.id IN :projectIds")
    long countInProjects(@Param("projectIds") Collection<Long> projectIds);

    @Query("SELECT COUNT(e) FROM Equipment e " +
            "WHERE e.status = :status " +
            "AND e.site.project.id IN :projectIds")
    long countByStatusInProjects(@Param("status") String status,
                                 @Param("projectIds") Collection<Long> projectIds);

    // every piece of equipment whose energy can be compared with the forecast
    // NOT scoped: used by the internal anomaly detection job
    @Query("SELECT e FROM Equipment e JOIN FETCH e.site s "
        + "WHERE e.capacityKw IS NOT NULL "
        + "AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
    List<Equipment> findAllForPerformanceAnalysis();


    // ---- map: number of pieces of equipment per site, in one query ----
    @Query("SELECT new com.solarvision.backend.equipment.SiteEquipmentCount(e.site.id, COUNT(e)) " +
        "FROM Equipment e " +
        "GROUP BY e.site.id")
    List<SiteEquipmentCount> countBySite();

    /** The project an equipment belongs to (equipment -> site -> project). */
    @Query("select e.site.project.id from Equipment e where e.id = :equipmentId")
    Optional<Long> findProjectIdForEquipment(@Param("equipmentId") Long equipmentId);

    /** The distinct projects that a group of equipment belongs to. */
    @Query("select distinct e.site.project.id from Equipment e where e.id in :equipmentIds")
    List<Long> findProjectIdsForEquipment(@Param("equipmentIds") Collection<Long> equipmentIds);

    /** Total capacity of the active inverters (the most power the sites can deliver). */
    @Query("select sum(e.capacityKw) from Equipment e where e.equipmentType = 'INVERTER' and e.status = 'ACTIVE'")
    BigDecimal sumActiveInverterCapacity();

    /** Same as above, only for the given projects. */
    @Query("""
            select sum(e.capacityKw) from Equipment e
            where e.equipmentType = 'INVERTER' and e.status = 'ACTIVE'
              and e.site.project.id in :projectIds
            """)
    BigDecimal sumActiveInverterCapacityInProjects(@Param("projectIds") Set<Long> projectIds);
}

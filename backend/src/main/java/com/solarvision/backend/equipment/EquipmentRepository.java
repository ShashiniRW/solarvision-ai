package com.solarvision.backend.equipment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
}
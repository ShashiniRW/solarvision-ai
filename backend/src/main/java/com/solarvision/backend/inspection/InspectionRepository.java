package com.solarvision.backend.inspection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    // the equipment is loaded together with the inspection
    @EntityGraph(attributePaths = "equipment")
    Optional<Inspection> findWithEquipmentById(Long id);

    @EntityGraph(attributePaths = "equipment")
    List<Inspection> findAllByOrderByScheduledAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "equipment")
    List<Inspection> findByStatusOrderByScheduledAtDesc(InspectionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "equipment")
    List<Inspection> findByEquipmentIdOrderByScheduledAtDesc(Long equipmentId, Pageable pageable);

    @EntityGraph(attributePaths = "equipment")
    List<Inspection> findByStatusAndEquipmentIdOrderByScheduledAtDesc(InspectionStatus status,
                                                                      Long equipmentId,
                                                                      Pageable pageable);

    // scoped list - only inspections whose equipment belongs to one of the given projects
    @EntityGraph(attributePaths = "equipment")
    @Query("SELECT i FROM Inspection i " +
            "WHERE i.equipment.site.project.id IN :projectIds " +
            "ORDER BY i.scheduledAt DESC")
    List<Inspection> findInProjects(@Param("projectIds") Collection<Long> projectIds,
                                    Pageable pageable);

    // same, filtered by status
    @EntityGraph(attributePaths = "equipment")
    @Query("SELECT i FROM Inspection i " +
            "WHERE i.status = :status " +
            "AND i.equipment.site.project.id IN :projectIds " +
            "ORDER BY i.scheduledAt DESC")
    List<Inspection> findByStatusInProjects(@Param("status") InspectionStatus status,
                                            @Param("projectIds") Collection<Long> projectIds,
                                            Pageable pageable);


    long countByStatus(InspectionStatus status);

    // scheduled inspections whose time has already passed
    long countByStatusAndScheduledAtBefore(InspectionStatus status, OffsetDateTime time);

    // NEW (dashboard): count by status, limited to the given projects
    @Query("SELECT COUNT(i) FROM Inspection i " +
            "WHERE i.status = :status " +
            "AND i.equipment.site.project.id IN :projectIds")
    long countByStatusInProjects(@Param("status") InspectionStatus status,
                                 @Param("projectIds") Collection<Long> projectIds);

    // NEW (dashboard): overdue inspections, limited to the given projects
    @Query("SELECT COUNT(i) FROM Inspection i " +
            "WHERE i.status = :status " +
            "AND i.scheduledAt < :time " +
            "AND i.equipment.site.project.id IN :projectIds")
    long countByStatusAndScheduledAtBeforeInProjects(@Param("status") InspectionStatus status,
                                                     @Param("time") OffsetDateTime time,
                                                     @Param("projectIds") Collection<Long> projectIds);
}
package com.solarvision.backend.inspection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
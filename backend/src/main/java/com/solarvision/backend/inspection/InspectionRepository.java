package com.solarvision.backend.inspection;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    // loads the equipment together with the inspection
    @EntityGraph(attributePaths = "equipment")
    Optional<Inspection> findWithEquipmentById(Long id);
}
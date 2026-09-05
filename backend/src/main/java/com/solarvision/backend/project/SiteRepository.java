package com.solarvision.backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    @Query("SELECT s FROM Site s LEFT JOIN FETCH s.project p LEFT JOIN FETCH p.organization LEFT JOIN FETCH p.customer WHERE s.project.id = :projectId")
    List<Site> findByProjectIdWithDetails(Long projectId);

    @Query("SELECT s FROM Site s LEFT JOIN FETCH s.project p LEFT JOIN FETCH p.organization LEFT JOIN FETCH p.customer WHERE s.id = :id")
    Optional<Site> findByIdWithDetails(Long id);
}
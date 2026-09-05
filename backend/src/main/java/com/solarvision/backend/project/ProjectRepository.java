package com.solarvision.backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.organization LEFT JOIN FETCH p.customer")
    List<Project> findAllWithDetails();

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.organization LEFT JOIN FETCH p.customer WHERE p.id = :id")
    Optional<Project> findByIdWithDetails(Long id);
}
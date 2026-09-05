package com.solarvision.backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Long> {

    @Query("SELECT pa FROM ProjectAssignment pa " +
            "LEFT JOIN FETCH pa.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "LEFT JOIN FETCH pa.user u " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE pa.project.id = :projectId")
    List<ProjectAssignment> findByProjectIdWithDetails(Long projectId);

    @Query("SELECT pa FROM ProjectAssignment pa " +
            "LEFT JOIN FETCH pa.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "LEFT JOIN FETCH pa.user u " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE pa.user.id = :userId")
    List<ProjectAssignment> findByUserIdWithDetails(Long userId);

    @Query("SELECT pa FROM ProjectAssignment pa " +
            "LEFT JOIN FETCH pa.project p " +
            "LEFT JOIN FETCH p.organization " +
            "LEFT JOIN FETCH p.customer " +
            "LEFT JOIN FETCH pa.user u " +
            "LEFT JOIN FETCH u.roles " +
            "WHERE pa.id = :id")
    Optional<ProjectAssignment> findByIdWithDetails(Long id);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
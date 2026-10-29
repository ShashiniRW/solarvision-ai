package com.solarvision.backend.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.organization LEFT JOIN FETCH p.customer")
    List<Project> findAllWithDetails();

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.organization LEFT JOIN FETCH p.customer WHERE p.id = :id")
    Optional<Project> findByIdWithDetails(Long id);


    // ids of the projects a user may see: their organisation's projects (staff only),
    // projects where they are the customer, and projects they are assigned to
    @Query("""
            SELECT p.id FROM Project p
            WHERE (:includeOrganization = true AND p.organization.id = :organizationId)
               OR p.customer.id = :userId
               OR EXISTS (SELECT 1 FROM ProjectAssignment a
                          WHERE a.project = p AND a.user.id = :userId)
            """)
    List<Long> findAccessibleProjectIds(@Param("userId") Long userId,
                                        @Param("organizationId") Long organizationId,
                                        @Param("includeOrganization") boolean includeOrganization);
}
package com.solarvision.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    /** Every user with roles and organization loaded in ONE query (for the Users page, no N+1). */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.roles
            JOIN FETCH u.organization
            ORDER BY u.firstName, u.lastName
            """)
    List<User> findAllWithRolesAndOrganization();
}

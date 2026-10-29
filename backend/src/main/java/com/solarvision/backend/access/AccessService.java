package com.solarvision.backend.access;

import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.project.ProjectRepository;
import com.solarvision.backend.user.Role;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides which projects (and therefore which sites, equipment, readings, alerts and
 * inspections) a user may see.
 *
 * A user may see a project when ANY of these is true:
 *  - the user is an ADMIN
 *  - the user is staff (MANAGER, ENGINEER or TECHNICIAN) in the project's organisation
 *  - the user is the project's customer
 *  - the user is assigned to the project
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ENGINEER", "TECHNICIAN");

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public ProjectScope scopeFor(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        if (roles.contains("ADMIN")) {
            return ProjectScope.everything();
        }

        boolean staff = roles.stream().anyMatch(STAFF_ROLES::contains);
        Long organizationId = user.getOrganization().getId();

        return ProjectScope.only(new HashSet<>(
                projectRepository.findAccessibleProjectIds(user.getId(), organizationId, staff)));
    }

    /** The scope of the user who is making the current request. */
    @Transactional(readOnly = true)
    public ProjectScope currentScope() {
        return scopeFor(currentEmail());
    }

    /** Throws "Project not found" when the current user may not see the project. */
    public void requireProjectAccess(Long projectId) {
        if (!currentScope().allows(projectId)) {
            // 404 instead of 403: a stranger cannot tell whether the project exists
            throw new ResourceNotFoundException("Project not found");
        }
    }

    private String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        return authentication.getName();
    }
}
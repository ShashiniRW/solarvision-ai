package com.solarvision.backend.access;

import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.organization.Organization;
import com.solarvision.backend.project.ProjectRepository;
import com.solarvision.backend.user.Role;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AccessService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private User givenUser(Long id, Long organizationId, String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            Role role = mock(Role.class);
            lenient().when(role.getName()).thenReturn(name);
            roles.add(role);
        }
        Organization organization = mock(Organization.class);
        lenient().when(organization.getId()).thenReturn(organizationId);

        User user = new User();
        user.setId(id);
        user.setOrganization(organization);
        user.setRoles(roles);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        return user;
    }

    private void loggedInAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    // ---------- scopeFor ----------

    @Test
    void admin_seesEverything_withoutAskingForProjects() {
        givenUser(1L, 5L, "ADMIN");

        ProjectScope scope = service.scopeFor(EMAIL);

        assertThat(scope.unrestricted()).isTrue();
        assertThat(scope.allows(999L)).isTrue();
        verify(projectRepository, never()).findAccessibleProjectIds(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void adminWhoIsAlsoCustomer_stillSeesEverything() {
        givenUser(1L, 5L, "ADMIN", "CUSTOMER");

        assertThat(service.scopeFor(EMAIL).unrestricted()).isTrue();
    }

    @Test
    void staff_includesTheProjectsOfTheirOrganisation() {
        givenUser(7L, 5L, "ENGINEER");
        when(projectRepository.findAccessibleProjectIds(7L, 5L, true)).thenReturn(List.of(1L, 2L));

        ProjectScope scope = service.scopeFor(EMAIL);

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.allows(1L)).isTrue();
        assertThat(scope.allows(2L)).isTrue();
        assertThat(scope.allows(3L)).isFalse();
    }

    @Test
    void customer_doesNotGetTheProjectsOfTheOrganisation() {
        givenUser(8L, 5L, "CUSTOMER");
        // includeOrganization must be false for a customer
        when(projectRepository.findAccessibleProjectIds(8L, 5L, false)).thenReturn(List.of(4L));

        ProjectScope scope = service.scopeFor(EMAIL);

        assertThat(scope.allows(4L)).isTrue();
        assertThat(scope.allows(1L)).isFalse();
    }

    @Test
    void customerWithoutProjects_seesNothing() {
        givenUser(9L, 5L, "CUSTOMER");
        when(projectRepository.findAccessibleProjectIds(9L, 5L, false)).thenReturn(List.of());

        assertThat(service.scopeFor(EMAIL).allows(1L)).isFalse();
    }

    @Test
    void unknownUser_throwsNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.scopeFor(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    // ---------- requireProjectAccess ----------

    @Test
    void requireProjectAccess_passes_whenTheProjectIsInScope() {
        givenUser(7L, 5L, "ENGINEER");
        when(projectRepository.findAccessibleProjectIds(7L, 5L, true)).thenReturn(List.of(1L));
        loggedInAs(EMAIL);

        assertThatCode(() -> service.requireProjectAccess(1L)).doesNotThrowAnyException();
    }

    @Test
    void requireProjectAccess_hidesProjectsOutsideTheScope() {
        givenUser(7L, 5L, "ENGINEER");
        when(projectRepository.findAccessibleProjectIds(7L, 5L, true)).thenReturn(List.of(1L));
        loggedInAs(EMAIL);

        assertThatThrownBy(() -> service.requireProjectAccess(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void requireProjectAccess_failsWhenNobodyIsLoggedIn() {
        assertThatThrownBy(() -> service.requireProjectAccess(1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
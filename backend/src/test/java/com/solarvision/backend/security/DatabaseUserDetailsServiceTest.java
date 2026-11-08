package com.solarvision.backend.security;

import com.solarvision.backend.user.Role;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    private static final String EMAIL = "engineer@example.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService service;

    private Role role(String name) {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(name);
        return role;
    }

    @Test
    void loadsUserFromDatabase_withHashAndRoles() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("$2a$10$hash");
        user.setRoles(Set.of(role("ENGINEER"), role("CUSTOMER")));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername(EMAIL);

        assertThat(details.getUsername()).isEqualTo(EMAIL);
        assertThat(details.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ENGINEER", "ROLE_CUSTOMER");
    }

    @Test
    void unknownEmail_throwsUsernameNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }


    @Test
    void inactiveUser_isDisabled() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("$2a$10$hash");
        user.setStatus("INACTIVE");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername(EMAIL).isEnabled()).isFalse();
    }
}
package com.solarvision.backend.user;

import com.solarvision.backend.common.exception.InvalidCredentialsException;
import com.solarvision.backend.organization.OrganizationRepository;
import com.solarvision.backend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "engineer@example.com";
    private static final String PASSWORD = "correct-password";
    private static final String HASH = "stored-bcrypt-hash";

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService service;

    private User user(String status) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(HASH);
        user.setStatus(status);
        return user;
    }

    @Test
    void login_returnsToken_forCorrectCredentials() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user("ACTIVE")));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(jwtUtil.generateToken(EMAIL)).thenReturn("the-token");

        assertThat(service.login(EMAIL, PASSWORD)).isEqualTo("the-token");
    }

    @Test
    void login_rejectsWrongPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user("ACTIVE")));
        when(passwordEncoder.matches("wrong", HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.login(EMAIL, "wrong"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_rejectsUnknownEmail_withTheSameMessage_afterDoingThePasswordWork() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nobody@example.com", PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        // the password check still ran (against the dummy hash), so timing does not give the email away
        verify(passwordEncoder).matches(eq(PASSWORD), any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_rejectsInactiveAccount_evenWithCorrectPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user("INACTIVE")));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        assertThatThrownBy(() -> service.login(EMAIL, PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(jwtUtil, never()).generateToken(any());
    }
}
package com.solarvision.backend.user;

import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.user.dto.ChangePasswordRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String EMAIL = "engineer@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService service;

    private User userWithTemporaryPassword() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash("temp-hash");
        user.setMustChangePassword(true);
        return user;
    }

    @Test
    void changePassword_storesTheNewHash_andClearsMustChangePassword() {
        User user = userWithTemporaryPassword();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("TempPassword2345", "temp-hash")).thenReturn(true);
        when(passwordEncoder.encode("my-own-strong-password")).thenReturn("new-hash");

        service.changePassword(EMAIL, new ChangePasswordRequest("TempPassword2345", "my-own-strong-password"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.isMustChangePassword()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_refusesAWrongCurrentPassword() {
        User user = userWithTemporaryPassword();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "temp-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(EMAIL,
            new ChangePasswordRequest("wrong-password", "my-own-strong-password")))
            .isInstanceOf(ConflictException.class);
        assertThat(user.getPasswordHash()).isEqualTo("temp-hash");
        verify(passwordEncoder, never()).encode(any());
    }
}

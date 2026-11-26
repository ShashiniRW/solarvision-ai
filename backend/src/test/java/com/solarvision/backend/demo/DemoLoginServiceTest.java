package com.solarvision.backend.demo;

import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.security.JwtUtil;
import com.solarvision.backend.user.User;
import com.solarvision.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoLoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private DemoLoginService service;

    @Test
    void returnsATokenForAnActiveDemoUser() {
        User manager = mock(User.class);
        when(manager.isActive()).thenReturn(true);
        when(manager.getEmail()).thenReturn(DemoDataSeeder.MANAGER_EMAIL);
        when(userRepository.findByEmail(DemoDataSeeder.MANAGER_EMAIL)).thenReturn(Optional.of(manager));
        when(jwtUtil.generateToken(DemoDataSeeder.MANAGER_EMAIL)).thenReturn("demo-token");

        assertThat(service.login(DemoRole.MANAGER)).isEqualTo("demo-token");
    }

    @Test
    void refusesADeactivatedDemoUser() {
        User customer = mock(User.class);
        when(customer.isActive()).thenReturn(false);
        when(userRepository.findByEmail(DemoDataSeeder.CUSTOMER_EMAIL)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.login(DemoRole.CUSTOMER))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void refusesWhenTheDemoDataWasNeverCreated() {
        when(userRepository.findByEmail(DemoDataSeeder.ENGINEER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(DemoRole.ENGINEER))
            .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void adminCanNeverBeADemoRole() {
        assertThat(DemoRole.values()).extracting(Enum::name).doesNotContain("ADMIN");
    }
}

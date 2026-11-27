package com.solarvision.backend.user;

import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.organization.Organization;
import com.solarvision.backend.user.dto.CreateUserRequest;
import com.solarvision.backend.user.dto.UpdateUserRequest;
import com.solarvision.backend.user.dto.UserSummary;
import com.solarvision.backend.user.dto.UserWithTemporaryPassword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    private static final String ADMIN_EMAIL = "admin@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TemporaryPasswordGenerator passwordGenerator;

    @InjectMocks
    private UserAdminService service;

    private static Organization company() {
        Organization organization = mock(Organization.class);
        when(organization.getName()).thenReturn("SolarVision Energy");
        return organization;
    }

    private static User user(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setStatus("ACTIVE");
        return user;
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    @Test
    void createUser_storesOnlyTheHash_andMustChangePassword_andReturnsTheTemporaryPasswordOnce() {
        User admin = user(1L, ADMIN_EMAIL);
        admin.setOrganization(company());

        when(userRepository.findByEmail("new.engineer@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ENGINEER")).thenReturn(Optional.of(role("ENGINEER")));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        when(passwordGenerator.generate()).thenReturn("TempPassword2345");
        when(passwordEncoder.encode("TempPassword2345")).thenReturn("bcrypt-hash");

        UserWithTemporaryPassword result = service.createUser(
            new CreateUserRequest("Amal", "Perera", "  New.Engineer@Example.com ", " ", "ENGINEER"),
            ADMIN_EMAIL);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getValue().getPhone()).isNull();

        assertThat(result.temporaryPassword()).isEqualTo("TempPassword2345");
        assertThat(result.user().email()).isEqualTo("new.engineer@example.com");
        assertThat(result.user().roles()).containsExactly("ENGINEER");
        assertThat(result.user().organizationName()).isEqualTo("SolarVision Energy");
        assertThat(result.user().mustChangePassword()).isTrue();
    }

    @Test
    void createUser_rejectsAnEmailThatIsAlreadyUsed() {
        when(userRepository.findByEmail("taken@example.com"))
            .thenReturn(Optional.of(user(5L, "taken@example.com")));

        assertThatThrownBy(() -> service.createUser(
            new CreateUserRequest("A", "B", "taken@example.com", null, "MANAGER"), ADMIN_EMAIL))
            .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void setActive_neverLetsAnAdminDeactivateThemselves() {
        User admin = user(1L, ADMIN_EMAIL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.setActive(1L, false, ADMIN_EMAIL))
            .isInstanceOf(ConflictException.class);
        assertThat(admin.getStatus()).isEqualTo("ACTIVE");
        verify(userRepository, never()).save(any());
    }

    @Test
    void setActive_deactivatesAnotherUser() {
        User technician = user(7L, "tech@example.com");
        technician.setOrganization(company());
        when(userRepository.findById(7L)).thenReturn(Optional.of(technician));

        UserSummary result = service.setActive(7L, false, ADMIN_EMAIL);

        assertThat(result.status()).isEqualTo("INACTIVE");
        verify(userRepository).save(technician);
    }

    @Test
    void resetPassword_replacesTheHash_andForcesAChangeAtNextSignIn() {
        User customer = user(9L, "customer@example.com");
        customer.setPasswordHash("old-hash");
        when(userRepository.findById(9L)).thenReturn(Optional.of(customer));
        when(passwordGenerator.generate()).thenReturn("NewTempPass6789x");
        when(passwordEncoder.encode("NewTempPass6789x")).thenReturn("new-hash");

        String temporaryPassword = service.resetPassword(9L);

        assertThat(temporaryPassword).isEqualTo("NewTempPass6789x");
        assertThat(customer.getPasswordHash()).isEqualTo("new-hash");
        assertThat(customer.isMustChangePassword()).isTrue();
    }

    @Test
    void updateUser_correctsTheNameAndPhone_andReplacesTheRole() {
        User engineer = user(7L, "amal.test@example.com");
        engineer.setFirstName("Perera");
        engineer.setLastName("Perera");
        engineer.setOrganization(company());
        when(userRepository.findById(7L)).thenReturn(Optional.of(engineer));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(role("MANAGER")));

        UserSummary result = service.updateUser(7L,
            new UpdateUserRequest(" Amal ", "Perera", "0771234567", "MANAGER"), ADMIN_EMAIL);

        assertThat(result.firstName()).isEqualTo("Amal");
        assertThat(result.phone()).isEqualTo("0771234567");
        assertThat(result.roles()).containsExactly("MANAGER");
        assertThat(result.email()).isEqualTo("amal.test@example.com"); // the email never changes
        verify(userRepository).save(engineer);
    }

    @Test
    void updateUser_neverLetsAnAdminRemoveTheirOwnAdminRole() {
        User admin = user(1L, ADMIN_EMAIL);
        admin.setFirstName("Shashini");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.updateUser(1L,
            new UpdateUserRequest("Someone", "Else", null, "ENGINEER"), ADMIN_EMAIL))
            .isInstanceOf(ConflictException.class);
        assertThat(admin.getFirstName()).isEqualTo("Shashini"); // nothing was changed
        verify(userRepository, never()).save(any());
    }
}

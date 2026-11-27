package com.solarvision.backend.user;

import com.solarvision.backend.common.exception.ConflictException;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.user.dto.CreateUserRequest;
import com.solarvision.backend.user.dto.UpdateUserRequest;
import com.solarvision.backend.user.dto.UserSummary;
import com.solarvision.backend.user.dto.UserWithTemporaryPassword;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * User management for administrators (the controller only lets ADMIN in).
 * Users are deactivated, never deleted, so history ("resolved by ...") stays correct.
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator passwordGenerator;

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userRepository.findAllWithRolesAndOrganization().stream()
            .map(UserSummary::from)
            .toList();
    }

    /** Creates an account in the administrator's company, with a one-time password. */
    @Transactional
    public UserWithTemporaryPassword createUser(CreateUserRequest request, String adminEmail) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("A user with this email already exists");
        }

        Role role = findRole(request.role());
        User admin = findByEmail(adminEmail);

        String temporaryPassword = passwordGenerator.generate();

        User user = new User();
        user.setOrganization(admin.getOrganization()); // one company: new people join the admin's company
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(blankToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword)); // only the BCrypt hash is stored
        user.setStatus("ACTIVE");
        user.setMustChangePassword(true);
        user.setRoles(new HashSet<>(Set.of(role)));

        userRepository.save(user);
        return new UserWithTemporaryPassword(UserSummary.from(user), temporaryPassword);
    }

    /** Corrects a user's name, phone or role. The email (sign-in name) cannot be changed. */
    @Transactional
    public UserSummary updateUser(Long userId, UpdateUserRequest request, String adminEmail) {
        User user = findById(userId);
        boolean editingSelf = user.getEmail().equalsIgnoreCase(adminEmail);

        // checked BEFORE changing anything: an admin can never remove their own admin role
        if (editingSelf && request.role() != null && !"ADMIN".equals(request.role())) {
            throw new ConflictException("You cannot remove your own administrator role");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(blankToNull(request.phone()));

        // role == null means "keep the current role(s)"
        if (request.role() != null) {
            user.setRoles(new HashSet<>(Set.of(findRole(request.role()))));
        }

        userRepository.save(user);
        return UserSummary.from(user);
    }

    /** Deactivate (active = false) or reactivate a user. A deactivated user loses access at once. */
    @Transactional
    public UserSummary setActive(Long userId, boolean active, String adminEmail) {
        User user = findById(userId);

        // an admin can never lock themselves out (the system must always keep an admin)
        if (!active && user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new ConflictException("You cannot deactivate your own account");
        }

        user.setStatus(active ? "ACTIVE" : "INACTIVE");
        userRepository.save(user);
        return UserSummary.from(user);
    }

    /** Replaces the password with a new one-time password. The old password stops working at once. */
    @Transactional
    public String resetPassword(Long userId) {
        User user = findById(userId);

        String temporaryPassword = passwordGenerator.generate();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        return temporaryPassword;
    }

    private Role findRole(String name) {
        return roleRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private User findById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

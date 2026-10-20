package com.solarvision.backend.user;

import com.solarvision.backend.common.exception.InvalidCredentialsException;
import com.solarvision.backend.common.exception.ResourceNotFoundException;
import com.solarvision.backend.organization.Organization;
import com.solarvision.backend.organization.OrganizationRepository;
import com.solarvision.backend.security.JwtUtil;
import com.solarvision.backend.user.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // A real hash of a throw-away password. Used so that a login attempt with an unknown
    // email takes as long as one with a known email (no timing clue about which emails exist).
    private final String dummyPasswordHash;

    public UserService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password");
    }

    public User registerUser(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Role customerRole = roleRepository.findAll().stream()
                .filter(role -> role.getName().equals("CUSTOMER"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Default CUSTOMER role not found"));

        User user = new User();
        user.setOrganization(organization);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setStatus("ACTIVE");

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Same amount of work as for a real user, then the same generic failure
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }

        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        boolean accountActive = "ACTIVE".equals(user.getStatus());

        if (!passwordMatches || !accountActive) {
            throw new InvalidCredentialsException();
        }

        return jwtUtil.generateToken(user.getEmail());
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
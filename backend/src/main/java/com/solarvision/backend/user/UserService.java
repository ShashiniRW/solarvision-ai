package com.solarvision.backend.user;

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
    }

    public User registerUser(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return jwtUtil.generateToken(user.getEmail());
    }
}
package com.solarvision.backend.user.dto;

import com.solarvision.backend.user.Role;
import com.solarvision.backend.user.User;

import java.time.LocalDateTime;
import java.util.List;

/** One row of the Users page. Never contains the password hash. */
public record UserSummary(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    String status,
    List<String> roles,
    String organizationName,
    boolean mustChangePassword,
    LocalDateTime createdAt
) {
    public static UserSummary from(User user) {
        return new UserSummary(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhone(),
            user.getStatus(),
            user.getRoles().stream().map(Role::getName).sorted().toList(),
            user.getOrganization().getName(),
            user.isMustChangePassword(),
            user.getCreatedAt());
    }
}

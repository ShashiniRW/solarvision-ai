package com.solarvision.backend.user.dto;

import com.solarvision.backend.user.Role;
import com.solarvision.backend.user.User;

import java.util.List;

/** The signed-in user's own account (used by the frontend on every page). */
public record AccountResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    List<String> roles,
    boolean mustChangePassword
) {
    public static AccountResponse from(User user) {
        return new AccountResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRoles().stream().map(Role::getName).sorted().toList(),
            user.isMustChangePassword());
    }
}

package com.solarvision.backend.user.dto;

import com.solarvision.backend.user.Role;
import com.solarvision.backend.user.User;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        List<String> roles
) {
    public static CurrentUserResponse fromEntity(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();

        return new CurrentUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleNames
        );
    }
}
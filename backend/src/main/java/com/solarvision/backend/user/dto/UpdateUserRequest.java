package com.solarvision.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What an administrator may change. The email is not here on purpose: it is the sign-in name.
 * role may be null = keep the current role(s) (used when admins edit their own details).
 */
public record UpdateUserRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Size(max = 30) String phone,
    @Pattern(regexp = "ADMIN|MANAGER|ENGINEER|TECHNICIAN|CUSTOMER", message = "Unknown role")
    String role
) {
}

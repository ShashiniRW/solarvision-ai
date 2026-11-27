package com.solarvision.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What an administrator fills in to create an account. No password: the system creates one. */
public record CreateUserRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 30) String phone,
    @NotBlank
    @Pattern(regexp = "ADMIN|MANAGER|ENGINEER|TECHNICIAN|CUSTOMER", message = "Unknown role")
    String role
) {
}

package com.solarvision.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 12, max = 128, message = "The new password must have 12 to 128 characters")
    String newPassword
) {
}

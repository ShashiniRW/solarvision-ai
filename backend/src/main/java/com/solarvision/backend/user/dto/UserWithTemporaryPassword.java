package com.solarvision.backend.user.dto;

/** Returned once, right after creating a user. The temporary password is never shown again. */
public record UserWithTemporaryPassword(UserSummary user, String temporaryPassword) {
}

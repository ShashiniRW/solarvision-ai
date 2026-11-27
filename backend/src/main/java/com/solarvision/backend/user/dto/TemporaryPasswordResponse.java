package com.solarvision.backend.user.dto;

/** Returned once, right after a password reset. */
public record TemporaryPasswordResponse(String temporaryPassword) {
}

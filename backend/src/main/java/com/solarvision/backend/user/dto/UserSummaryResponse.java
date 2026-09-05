package com.solarvision.backend.user.dto;

public record UserSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status
) {}
package com.ticketmarket.dto;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String nickname,
        String roleCode,
        boolean realNameVerified
) {
}

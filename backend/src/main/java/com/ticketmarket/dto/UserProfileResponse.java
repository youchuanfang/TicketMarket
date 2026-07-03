package com.ticketmarket.dto;

public record UserProfileResponse(
        Long userId,
        String username,
        String nickname,
        String roleCode,
        boolean realNameVerified,
        String phoneMasked,
        String email,
        String idCardMasked
) {
}

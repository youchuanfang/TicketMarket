package com.ticketmarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "长度至少为 6 位") String password,
        @NotBlank String nickname
) {
}

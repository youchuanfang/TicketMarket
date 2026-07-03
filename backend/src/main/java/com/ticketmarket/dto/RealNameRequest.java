package com.ticketmarket.dto;

import jakarta.validation.constraints.NotBlank;

public record RealNameRequest(
        @NotBlank String realName,
        @NotBlank String idCard
) {
}

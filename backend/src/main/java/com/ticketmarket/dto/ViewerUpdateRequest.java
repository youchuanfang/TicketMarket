package com.ticketmarket.dto;

import jakarta.validation.constraints.NotBlank;

public record ViewerUpdateRequest(
        @NotBlank String name,
        @NotBlank String idCard,
        @NotBlank String phone
) {
}

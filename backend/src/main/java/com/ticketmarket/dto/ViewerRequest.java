package com.ticketmarket.dto;

import jakarta.validation.constraints.NotBlank;

public record ViewerRequest(
        @NotBlank String name,
        @NotBlank String idCard,
        @NotBlank String phone
) {
}

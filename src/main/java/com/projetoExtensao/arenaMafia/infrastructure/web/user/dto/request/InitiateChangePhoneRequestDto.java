package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InitiateChangePhoneRequestDto(
    @NotBlank(message = "PHONE_REQUIRED")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "PHONE_INVALID_FORMAT")
        String newPhone) {}

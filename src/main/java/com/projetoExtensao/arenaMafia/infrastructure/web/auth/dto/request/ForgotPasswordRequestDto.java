package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordRequestDto(
    @NotBlank(message = "PHONE_REQUIRED")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "PHONE_INVALID_FORMAT")
        String phone) {}

package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
    @NotBlank(message = "USERNAME_REQUIRED")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "USERNAME_INVALID_FORMAT")
        @Size(min = 3, max = 50, message = "USERNAME_INVALID_LENGTH")
        String username,
    @NotBlank(message = "PASSWORD_REQUIRED")
        @Size(min = 6, max = 20, message = "PASSWORD_INVALID_LENGTH")
        @Pattern(regexp = "^\\S+$", message = "PASSWORD_NO_WHITESPACE")
        String password) {}

package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequestDto(
    @NotBlank(message = "USERNAME_REQUIRED")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "USERNAME_INVALID_FORMAT")
        @Size(min = 3, max = 50, message = "USERNAME_INVALID_LENGTH")
        String username) {}

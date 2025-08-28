package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResendCodeRequestDto(
    @NotBlank(message = "Por favor, informe seu telefone.")
        @Pattern(
            regexp = "^\\+\\d{10,15}$",
            message = "O telefone deve estar no formato internacional (ex: +5547999999999).")
        String phone) {}

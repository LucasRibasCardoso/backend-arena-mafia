package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompletePhoneChangeRequestDTO(
    @NotBlank(message = "O código de verificação é obrigatório.")
        @Pattern(
            regexp = "^\\d{6}$",
            message = "O código de verificação deve conter exatamente 6 dígitos numéricos.")
        String code) {}

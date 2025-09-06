package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyAccountRequestDto(
    @NotNull(message = "O identificador do usuário é obrigatório.") String userId,
    @NotBlank(message = "O código de verificação é obrigatório.")
        @Pattern(
            regexp = "^\\d{6}$",
            message = "O código de verificação deve conter exatamente 6 dígitos numéricos.")
        String code) {}

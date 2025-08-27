package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResendCodeRequestDto(
    @NotBlank(message = "O nome de usuário é obrigatório.")
        @Size(min = 3, max = 20, message = "O nome de usuário deve ter entre 3 e 20 caracteres.")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]{3,20}$",
            message = "O nome de usuário deve conter apenas letras, números e underscore (_).")
        String username) {}

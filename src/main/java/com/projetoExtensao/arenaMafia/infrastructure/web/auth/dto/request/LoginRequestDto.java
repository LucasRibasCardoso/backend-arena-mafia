package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
    @NotBlank(message = "O usuario é obrigatório")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]{3,20}$",
            message = "O nome de usuário deve conter apenas letras, números e underscore (_).")
        @Size(min = 4, max = 50, message = "O campo usuário deve ter entre 4 e 50 caracteres")
        String username,
    @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres")
        @Pattern(regexp = "^\\S+$", message = "A senha não pode conter espaços em branco.")
        String password) {}

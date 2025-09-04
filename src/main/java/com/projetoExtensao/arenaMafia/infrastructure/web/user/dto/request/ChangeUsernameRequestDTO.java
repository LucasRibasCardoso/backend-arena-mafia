package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequestDTO(
    @NotBlank(message = "Por favor, informe um novo nome de usuário.")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]{3,20}$",
            message = "O nome de usuário deve conter apenas letras, números e underscore (_).")
        @Size(min = 3, max = 20, message = "O nome de usuário deve ter entre 3 e 20 caracteres.")
        String username) {}

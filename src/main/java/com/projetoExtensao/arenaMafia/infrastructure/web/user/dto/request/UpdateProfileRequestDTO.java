package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDTO(
    @Size(min = 3, max = 100, message = "O nome completo deve ter entre 3 e 100 caracteres.")
        @NotBlank(message = "Por favor, informe seu nome completo.")
        String fullName) {}

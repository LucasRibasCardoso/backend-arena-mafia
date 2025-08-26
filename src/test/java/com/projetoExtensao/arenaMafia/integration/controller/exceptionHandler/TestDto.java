package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestDto(
    @NotBlank(message = "O usuario é obrigatório")
        @Size(min = 4, max = 50, message = "O campo usuário deve ter entre 4 e 50 caracteres")
        String fieldTestUsername) {}

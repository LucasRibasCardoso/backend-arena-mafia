package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
    @Size(min = 3, max = 100, message = "O nome completo deve ter entre 3 e 100 caracteres.")
        String fullName) {}

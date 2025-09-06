package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResendCodeRequestDto(
    @NotBlank(message = "O identificador do usuário é obrigatório.") String userId) {}

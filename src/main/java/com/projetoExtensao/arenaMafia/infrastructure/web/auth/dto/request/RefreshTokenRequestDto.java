package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
    @NotBlank(message = "O refresh token não pode ser nulo ou vazio.") String refreshToken) {}

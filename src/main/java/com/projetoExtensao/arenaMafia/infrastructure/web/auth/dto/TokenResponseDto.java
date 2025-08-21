package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto;

import java.time.LocalDateTime;

public record TokenResponseDto(
    String username,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    String accessToken,
    String refreshToken) {}

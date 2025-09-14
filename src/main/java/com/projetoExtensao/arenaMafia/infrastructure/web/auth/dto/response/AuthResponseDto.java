package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response;

public record AuthResponseDto(
    String userId,
    String phone,
    String username,
    String fullName,
    String role,
    String accessToken) {}

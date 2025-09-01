package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response;

public record TokenResponseDto(
    String phone, String username, String fullName, String role, String accessToken) {}

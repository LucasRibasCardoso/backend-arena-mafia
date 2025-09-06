package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response;

public record SignupResponseDto(
    String userId, String username, String fullName, String phone, String status, String message) {}

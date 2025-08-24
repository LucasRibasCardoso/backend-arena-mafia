package com.projetoExtensao.arenaMafia.application.port.gateway.auth;

public record AuthResult(String username, String accessToken, String refreshToken) {}

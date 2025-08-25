package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

public record AuthResult(String username, String accessToken, String refreshToken) {}

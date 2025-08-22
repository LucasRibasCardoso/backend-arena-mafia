package com.projetoExtensao.arenaMafia.application.port.gateway;

public record AuthResult(String username, String accessToken, String refreshToken) {}

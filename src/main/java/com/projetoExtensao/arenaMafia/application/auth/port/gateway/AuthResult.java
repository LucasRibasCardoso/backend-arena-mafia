package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import com.projetoExtensao.arenaMafia.domain.model.User;

public record AuthResult(User user, String accessToken, String refreshToken) {}

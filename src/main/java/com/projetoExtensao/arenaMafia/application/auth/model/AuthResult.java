package com.projetoExtensao.arenaMafia.application.auth.model;

import com.projetoExtensao.arenaMafia.domain.model.User;

public record AuthResult(User user, String accessToken, String refreshToken) {}

package com.projetoExtensao.arenaMafia.application.port.gateway;

import com.projetoExtensao.arenaMafia.domain.model.User;

public interface AuthPort {
  User authenticate(String username, String password);

  AuthResult generateTokens(User user);
}

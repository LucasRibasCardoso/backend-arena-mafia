package com.projetoExtensao.arenaMafia.application.gateway;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;

public interface AuthPort {
  User authenticate(String username, String password);

  TokenResponseDto getTokens(String username, RoleEnum role);

  TokenResponseDto getRefreshToken(String username, RoleEnum role);
}

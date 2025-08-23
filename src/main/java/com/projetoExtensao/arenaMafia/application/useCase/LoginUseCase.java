package com.projetoExtensao.arenaMafia.application.useCase;

import com.projetoExtensao.arenaMafia.application.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;

public interface LoginUseCase {
  AuthResult login(LoginRequestDto loginRequestDto);
}

package com.projetoExtensao.arenaMafia.application.useCase;

import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;

public interface LoginUseCase {
  AuthResult execute(LoginRequestDto loginRequestDto);
}

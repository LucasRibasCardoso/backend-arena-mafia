package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.RefreshTokenRequestDto;

public interface RefreshTokenUseCase {
  AuthResult execute(RefreshTokenRequestDto refreshTokenRequestDto);
}

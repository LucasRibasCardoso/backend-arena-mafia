package com.projetoExtensao.arenaMafia.application.useCase;

import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.RefreshTokenRequestDto;

public interface RefreshTokenUseCase {
  AuthResult refreshToken(RefreshTokenRequestDto refreshTokenRequestDto);
}

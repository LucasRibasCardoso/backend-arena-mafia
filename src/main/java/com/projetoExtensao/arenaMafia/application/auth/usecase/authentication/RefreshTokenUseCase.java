package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.RefreshTokenRequestDto;

public interface RefreshTokenUseCase {
  AuthResult execute(RefreshTokenRequestDto refreshTokenRequestDto);
}

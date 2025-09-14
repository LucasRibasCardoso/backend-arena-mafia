package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;

public interface RefreshTokenUseCase {
  AuthResult execute(RefreshTokenVO refreshToken);
}

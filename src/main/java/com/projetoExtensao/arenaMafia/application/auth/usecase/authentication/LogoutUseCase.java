package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;

public interface LogoutUseCase {

  void execute(RefreshTokenVO refreshToken);
}

package com.projetoExtensao.arenaMafia.application.useCase;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;

public interface LoginUseCase {
  TokenResponseDto login(LoginRequestDto loginRequestDto);
}

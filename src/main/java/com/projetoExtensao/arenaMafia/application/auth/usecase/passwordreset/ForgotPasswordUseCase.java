package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;

public interface ForgotPasswordUseCase {
  ForgotPasswordResponseDto execute(ForgotPasswordRequestDto requestDto);
}

package com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;

public interface ForgotPasswordUseCase {
  void execute(ForgotPasswordRequestDto requestDto);
}

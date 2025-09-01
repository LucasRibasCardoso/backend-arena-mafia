package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;

public interface ForgotPasswordUseCase {
  void execute(ForgotPasswordRequestDto requestDto);
}

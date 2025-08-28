package com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;

public interface ResetPasswordUseCase {
  void execute(ResetPasswordRequestDto requestDto);
}

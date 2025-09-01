package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;

public interface ResetPasswordUseCase {
  void execute(ResetPasswordRequestDto requestDto);
}

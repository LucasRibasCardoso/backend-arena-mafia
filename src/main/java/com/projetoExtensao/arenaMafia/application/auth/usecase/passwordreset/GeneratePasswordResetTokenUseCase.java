package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;

public interface GeneratePasswordResetTokenUseCase {
  String execute(ValidateOtpRequestDto requestDto);
}

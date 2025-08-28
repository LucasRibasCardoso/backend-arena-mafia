package com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;

public interface GeneratePasswordResetTokenUseCase {
  String execute(ValidateOtpRequestDto requestDto);
}

package com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;

public interface ValidatePasswordResetOtpUseCase {
  PasswordResetTokenResponseDto execute(ValidateOtpRequestDto request);
}

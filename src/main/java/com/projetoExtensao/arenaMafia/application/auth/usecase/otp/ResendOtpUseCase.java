package com.projetoExtensao.arenaMafia.application.auth.usecase.otp;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;

public interface ResendOtpUseCase {
  void execute(OtpSessionId otpSessionId);
}

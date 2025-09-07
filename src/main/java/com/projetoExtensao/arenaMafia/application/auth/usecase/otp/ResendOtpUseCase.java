package com.projetoExtensao.arenaMafia.application.auth.usecase.otp;

public interface ResendOtpUseCase {
  void execute(String otpSessionId);
}

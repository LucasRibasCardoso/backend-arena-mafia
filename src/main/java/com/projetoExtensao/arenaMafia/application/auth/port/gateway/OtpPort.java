package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import java.util.UUID;

public interface OtpPort {
  String generateCodeOTP(UUID userId);

  void validateOtp(UUID uuid, String otpCode) throws InvalidOtpException;
}

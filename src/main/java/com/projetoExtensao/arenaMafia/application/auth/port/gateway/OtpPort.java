package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import java.util.UUID;

public interface OtpPort {
  String generateAndSaveOtp(UUID userId);

  boolean validateOtp(UUID uuid, String otpCode);
}

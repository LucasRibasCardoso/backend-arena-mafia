package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import java.util.Optional;
import java.util.UUID;

public interface OtpSessionPort {
  String generateOtpSession(UUID userId);

  Optional<UUID> findUserIdByOtpSessionId(String otpSessionId);
}

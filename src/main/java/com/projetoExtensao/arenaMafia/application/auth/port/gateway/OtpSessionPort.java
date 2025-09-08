package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import java.util.Optional;
import java.util.UUID;

public interface OtpSessionPort {
  OtpSessionId generateOtpSession(UUID userId);

  Optional<UUID> findUserIdByOtpSessionId(OtpSessionId otpSessionId);
}

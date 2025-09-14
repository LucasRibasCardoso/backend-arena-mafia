package com.projetoExtensao.arenaMafia.application.notification.gateway;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import java.util.UUID;

public interface OtpPort {
  OtpCode generateOtpCode(UUID userId);

  void validateOtp(UUID uuid, OtpCode code);
}

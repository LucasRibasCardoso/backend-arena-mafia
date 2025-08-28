package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import java.util.UUID;

public interface PasswordResetTokenPort {
  String save(UUID userId);

  UUID getUserIdByTokenOrElseThrow(String token);

  void delete(String token);
}

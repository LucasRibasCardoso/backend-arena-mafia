package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenPort {
  String generateToken(UUID userId);

  Optional<UUID> findUserIdByResetToken(String token);

  void delete(String token);
}

package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenPort {
  ResetToken generateToken(UUID userId);

  Optional<UUID> findUserIdByResetToken(ResetToken token);

  void delete(ResetToken token);
}

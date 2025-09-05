package com.projetoExtensao.arenaMafia.application.user.port.gateway;

import java.util.Optional;
import java.util.UUID;

public interface PendingPhoneChangePort {
  void save(UUID idCurrentUser, String newPhone);

  Optional<String> findPhoneByUserId(UUID userId);

  void deleteByUserId(UUID userId);
}

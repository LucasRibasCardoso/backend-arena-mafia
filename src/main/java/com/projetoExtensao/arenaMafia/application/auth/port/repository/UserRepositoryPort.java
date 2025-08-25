package com.projetoExtensao.arenaMafia.application.auth.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
  User save(User user);

  Optional<User> findByUsername(String username);

  Optional<User> findById(UUID id);

  boolean existsByUsernameOrPhone(String username, String phone);
}

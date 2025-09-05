package com.projetoExtensao.arenaMafia.application.user.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
  User save(User user);

  Optional<User> findByUsername(String username);

  Optional<User> findById(UUID id);

  Optional<User> findByPhone(String phone);

  boolean existsByUsername(String username);

  boolean existsByPhone(String phone);

  List<User> findByStatusAndCreatedAtBefore(AccountStatus status, Instant dateTime);

  List<User> findByStatusAndUpdateAtBefore(AccountStatus status, Instant dateTime);

  void deleteAll(List<User> users);
}

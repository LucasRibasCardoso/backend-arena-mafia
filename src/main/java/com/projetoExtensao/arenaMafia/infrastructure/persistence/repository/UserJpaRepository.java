package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByUsername(String username);

  Optional<UserEntity> findById(UUID id);

  Optional<UserEntity> findByPhone(String phone);

  boolean existsByPhone(String phone);

  boolean existsByUsername(String username);
}

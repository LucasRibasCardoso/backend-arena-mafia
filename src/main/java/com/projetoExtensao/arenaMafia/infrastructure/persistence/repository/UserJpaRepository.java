package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByUsername(String username);

  Optional<UserEntity> findById(UUID id);

  boolean existsByUsername(String username);

  boolean existsByPhone(String phone);
}

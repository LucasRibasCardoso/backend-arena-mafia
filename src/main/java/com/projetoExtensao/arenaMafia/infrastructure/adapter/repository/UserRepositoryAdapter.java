package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final UserJpaRepository userJpaRepository;
  private final UserMapper userMapper;

  public UserRepositoryAdapter(UserJpaRepository userJpaRepository, UserMapper userMapper) {
    this.userJpaRepository = userJpaRepository;
    this.userMapper = userMapper;
  }

  @Override
  public boolean existsByUsername(String username) {
    return userJpaRepository.existsByUsername(username);
  }

  @Override
  public boolean existsByPhone(String phone) {
    return userJpaRepository.existsByPhone(phone);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findById(UUID id) {
    return userJpaRepository.findById(id).map(userMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByUsername(String username) {
    return userJpaRepository.findByUsername(username).map(userMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByPhone(String phone) {
    return userJpaRepository.findByPhone(phone).map(userMapper::toDomain);
  }

  @Override
  @Transactional
  public User save(User user) {
    UserEntity userEntity = userMapper.toEntity(user);
    UserEntity savedUserEntity = userJpaRepository.save(userEntity);
    return userMapper.toDomain(savedUserEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findByStatusAndCreatedAtBefore(AccountStatus status, Instant dateTime) {
    return userJpaRepository.findByStatusAndCreatedAtBefore(status, dateTime).stream()
        .map(userMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findByStatusAndUpdateAtBefore(AccountStatus status, Instant dateTime) {
    return userJpaRepository.findByStatusAndUpdatedAtBefore(status, dateTime).stream()
        .map(userMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public void deleteAll(List<User> users) {
    List<UserEntity> entitiesToDelete = users.stream().map(userMapper::toEntity).toList();
    userJpaRepository.deleteAllInBatch(entitiesToDelete);
  }
}

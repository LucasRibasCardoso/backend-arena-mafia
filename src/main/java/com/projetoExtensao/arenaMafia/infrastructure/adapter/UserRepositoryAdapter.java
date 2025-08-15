package com.projetoExtensao.arenaMafia.infrastructure.adapter;

import com.projetoExtensao.arenaMafia.application.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final UserJpaRepository userJpaRepository;
  private final UserMapper userMapper;

  public UserRepositoryAdapter(UserJpaRepository userJpaRepository, UserMapper userMapper) {
    this.userJpaRepository = userJpaRepository;
    this.userMapper = userMapper;
  }

  @Override
  public boolean existsByPhone(String phone) {
    return userJpaRepository.existsByPhone(phone);
  }

  @Override
  public boolean existsByUsername(String username) {
    return userJpaRepository.existsByUsername(username);
  }

  @Override
  public Optional<User> findById(UUID id) {
    return userJpaRepository.findById(id).map(userMapper::toDomain);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return userJpaRepository.findByUsername(username).map(userMapper::toDomain);
  }

  @Override
  public User save(User user) {
    UserEntity userEntity = userMapper.toEntity(user);
    UserEntity savedUserEntity = userJpaRepository.save(userEntity);
    return userMapper.toDomain(savedUserEntity);
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "id", source = "id")
  @Mapping(target = "username", source = "username")
  @Mapping(target = "fullName", source = "fullName")
  @Mapping(target = "phone", source = "phone")
  @Mapping(target = "passwordHash", source = "passwordHash")
  @Mapping(target = "accountNonLocked", source = "accountNonLocked")
  @Mapping(target = "enabled", source = "enabled")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "createdAt", source = "createdAt")
  User toDomain(UserEntity userEntity);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "username", source = "username")
  @Mapping(target = "fullName", source = "fullName")
  @Mapping(target = "phone", source = "phone")
  @Mapping(target = "passwordHash", source = "passwordHash")
  @Mapping(target = "accountNonLocked", source = "accountNonLocked")
  @Mapping(target = "enabled", source = "enabled")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "createdAt", source = "createdAt")
  UserEntity toEntity(User user);
}

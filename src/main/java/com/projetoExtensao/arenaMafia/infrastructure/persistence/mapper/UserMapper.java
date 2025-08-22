package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "username", source = "username")
  UserEntity toEntity(User user);

  default User toDomain(UserEntity entity) {
    if (entity == null) {
      return null;
    }
    return User.reconstitute(
        entity.getId(),
        entity.getUsername(),
        entity.getFullName(),
        entity.getPhone(),
        entity.getPasswordHash(),
        entity.isAccountNonLocked(),
        entity.isEnabled(),
        entity.getRole(),
        entity.getCreatedAt());
  }
}

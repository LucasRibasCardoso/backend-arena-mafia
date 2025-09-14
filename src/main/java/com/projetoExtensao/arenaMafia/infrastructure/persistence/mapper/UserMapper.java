package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

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
        entity.getStatus(),
        entity.getRole(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}

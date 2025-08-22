package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class})
public abstract class RefreshTokenMapper {

  @Autowired private UserMapper userMapper;

  @Mapping(target = "id", ignore = true)
  public abstract RefreshTokenEntity toEntity(RefreshToken domain);

  public RefreshToken toDomain(RefreshTokenEntity entity) {
    if (entity == null) {
      return null;
    }

    User userDomain = userMapper.toDomain(entity.getUser());
    RefreshTokenVO tokenVO = mapStringToVO(entity.getToken());

    return RefreshToken.reconstitute(
        tokenVO, entity.getExpiryDate(), userDomain, entity.getCreatedAt());
  }

  // Métodos auxiliares para converter entre RefreshTokenVO e String e vice-versa
  protected RefreshTokenVO mapStringToVO(String token) {
    if (token == null || token.isBlank()) {
      return null;
    }
    return RefreshTokenVO.fromString(token);
  }

  protected String mapVOToString(RefreshTokenVO vo) {
    if (vo == null) {
      return null;
    }
    return vo.toString();
  }
}

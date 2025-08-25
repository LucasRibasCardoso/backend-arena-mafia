package com.projetoExtensao.arenaMafia.application.auth.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {
  RefreshToken save(RefreshToken refreshToken);

  Optional<RefreshToken> findByToken(RefreshTokenVO token);

  void delete(RefreshToken refreshToken);

  void deleteByUser(User user);
}

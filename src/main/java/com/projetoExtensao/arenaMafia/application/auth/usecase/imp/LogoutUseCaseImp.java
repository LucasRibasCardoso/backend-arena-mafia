package com.projetoExtensao.arenaMafia.application.auth.usecase.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.LogoutUseCase;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogoutUseCaseImp implements LogoutUseCase {

  private final RefreshTokenRepositoryPort refreshTokenRepository;

  public LogoutUseCaseImp(RefreshTokenRepositoryPort refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Override
  public void execute(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(refreshToken);
    refreshTokenRepository.findByToken(refreshTokenVO).ifPresent(refreshTokenRepository::delete);
  }
}

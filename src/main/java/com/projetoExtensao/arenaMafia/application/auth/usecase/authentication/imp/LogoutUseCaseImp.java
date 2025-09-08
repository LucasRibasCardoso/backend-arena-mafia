package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.LogoutUseCase;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
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
  public void execute(RefreshTokenVO refreshToken) {
    if (refreshToken == null) {
      return;
    }
    refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);
  }
}

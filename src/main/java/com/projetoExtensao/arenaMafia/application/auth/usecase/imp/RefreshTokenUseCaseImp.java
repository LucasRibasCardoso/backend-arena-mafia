package com.projetoExtensao.arenaMafia.application.auth.usecase.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.RefreshTokenRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenUseCaseImp implements RefreshTokenUseCase {

  private final AuthPort authPort;
  private final RefreshTokenRepositoryPort refreshTokenRepository;

  public RefreshTokenUseCaseImp(
      AuthPort authPort, RefreshTokenRepositoryPort refreshTokenRepository) {
    this.authPort = authPort;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Override
  public AuthResult execute(RefreshTokenRequestDto requestDto) {
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(requestDto.refreshToken());
    RefreshToken refreshToken = getRefreshTokenOrElseThrow(refreshTokenVO);

    try {
      refreshToken.verifyIfNotExpired();
      return authPort.generateTokens(refreshToken.getUser());
    } catch (RefreshTokenExpiredException e) {
      refreshTokenRepository.delete(refreshToken);
      // Relança a exceção para ser tratada no GlobalExceptionHandler
      throw e;
    }
  }

  private RefreshToken getRefreshTokenOrElseThrow(RefreshTokenVO refreshTokenVO) {
    return refreshTokenRepository
        .findByToken(refreshTokenVO)
        .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token não encontrado."));
  }
}

package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
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
    validateIfOldRefreshTokenExists(requestDto.refreshToken());

    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(requestDto.refreshToken());
    RefreshToken refreshToken = getRefreshTokenOrElseThrow(refreshTokenVO);
    refreshToken.getUser().ensureAccountEnabled();

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

  private void validateIfOldRefreshTokenExists(String oldRefreshToken) {
    if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
      throw new RefreshTokenNotFoundException(
          "Sua sessão expirou. Por favor, faça login novamente.");
    }
  }
}

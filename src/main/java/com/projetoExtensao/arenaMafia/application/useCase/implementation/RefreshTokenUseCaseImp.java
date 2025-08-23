package com.projetoExtensao.arenaMafia.application.useCase.implementation;

import com.projetoExtensao.arenaMafia.application.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.useCase.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.RefreshTokenRequestDto;
import org.springframework.transaction.annotation.Transactional;

public class RefreshTokenUseCaseImp implements RefreshTokenUseCase {

  private final AuthPort authPort;
  private final RefreshTokenRepositoryPort refreshTokenRepository;

  public RefreshTokenUseCaseImp(
      AuthPort authPort, RefreshTokenRepositoryPort refreshTokenRepository) {
    this.authPort = authPort;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Override
  @Transactional
  public AuthResult refreshToken(RefreshTokenRequestDto refreshTokenRequestDto) {
    // Transforma uma string em um objeto RefreshTokenVO
    RefreshTokenVO refreshTokenVO =
        RefreshTokenVO.fromString(refreshTokenRequestDto.refreshToken());

    // Busca o RefreshToken no banco de dados
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenVO)
            .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token não encontrado."));

    // Verifica se o RefreshToken está expirado
    if (refreshToken.isExpired()) {
      refreshTokenRepository.delete(refreshToken);
      throw new RefreshTokenExpiredException("Refresh token expirado. Faça login novamente.");
    }

    return authPort.generateTokens(refreshToken.getUser());
  }
}

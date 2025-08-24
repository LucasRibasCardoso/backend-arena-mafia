package com.projetoExtensao.arenaMafia.unit.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthPort;
import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthResult;
import com.projetoExtensao.arenaMafia.application.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.useCase.implementation.RefreshTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenInvalidFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.RefreshTokenRequestDto;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para RefreshTokenUseCaseImp")
public class RefreshTokenUseCaseImpTest {

  @Mock private AuthPort authPort;

  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;

  @InjectMocks private RefreshTokenUseCaseImp refreshTokenUseCase;

  private User testUser;
  private RefreshTokenRequestDto requestDto;
  private RefreshTokenVO refreshTokenVO;

  @BeforeEach
  void setUp() {
    testUser = User.create("testuser", "Test User", "5547912345678", "hash");
    String tokenString = RefreshTokenVO.generate().toString();
    requestDto = new RefreshTokenRequestDto(tokenString);
    refreshTokenVO = RefreshTokenVO.fromString(tokenString);
  }

  @Test
  @DisplayName("Deve renovar os tokens com sucesso para um refresh token válido")
  void refreshToken_shouldSucceedForValidToken() {
    // Arrange
    RefreshToken validRefreshToken = RefreshToken.create(30L, testUser);
    AuthResult expectedAuthResult =
        new AuthResult("testuser", "new-access-token", "new-refresh-token");

    when(refreshTokenRepository.findByToken(refreshTokenVO))
        .thenReturn(Optional.of(validRefreshToken));
    when(authPort.generateTokens(testUser)).thenReturn(expectedAuthResult);

    // Act
    AuthResult actualAuthResult = refreshTokenUseCase.refreshToken(requestDto);

    // Assert
    assertThat(actualAuthResult).isNotNull();
    assertThat(actualAuthResult.accessToken()).isEqualTo("new-access-token");

    verify(authPort, times(1)).generateTokens(testUser);
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenNotFoundException quando o token não for encontrado")
  void refreshToken_shouldThrowNotFoundExceptionWhenTokenIsMissing() {
    // Arrange
    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.refreshToken(requestDto))
        .isInstanceOf(RefreshTokenNotFoundException.class)
        .hasMessage("Refresh token não encontrado.");

    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenExpiredException quando o token estiver expirado")
  void refreshToken_shouldThrowExpiredExceptionForExpiredToken() {
    // Arrange
    RefreshToken expiredRefreshToken = RefreshToken.create(-1L, testUser);

    when(refreshTokenRepository.findByToken(refreshTokenVO))
        .thenReturn(Optional.of(expiredRefreshToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.refreshToken(requestDto))
        .isInstanceOf(RefreshTokenExpiredException.class)
        .hasMessage("Refresh token expirado. Faça login novamente.");

    verify(refreshTokenRepository, times(1)).delete(expiredRefreshToken);
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar RefreshTokenInvalidFormatException quando o token tiver formato inválido")
  void refreshToken_shouldThrowInvalidFormatExceptionForMalformedToken() {
    // Arrange
    RefreshTokenRequestDto invalidRequestDto = new RefreshTokenRequestDto("invalid-token");

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.refreshToken(invalidRequestDto))
        .isInstanceOf(RefreshTokenInvalidFormatException.class)
        .hasMessage("Formato inválido para o refresh token.");

    verify(refreshTokenRepository, never()).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }
}

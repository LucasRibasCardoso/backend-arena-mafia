package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.RefreshTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para RefreshTokenUseCase")
public class RefreshTokenUseCaseTest {

  @Mock private AuthPort authPort;
  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
  @InjectMocks private RefreshTokenUseCaseImp refreshTokenUseCase;

  private final RefreshTokenVO refreshTokenVO = RefreshTokenVO.generate();

  @Test
  @DisplayName("Deve renovar os tokens com sucesso quando for enviado um refresh token válido")
  void execute_shouldReturnTokens() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    RefreshToken refreshToken = TestDataProvider.createRefreshToken(user);
    AuthResult authResult = new AuthResult(user, "access-token", refreshTokenVO);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));
    when(authPort.generateTokens(user)).thenReturn(authResult);

    // Act
    AuthResult response = refreshTokenUseCase.execute(refreshTokenVO);

    // Assert
    assertThat(response.user()).isEqualTo(authResult.user());
    assertThat(response.refreshToken()).isEqualTo(authResult.refreshToken());
    assertThat(response.accessToken()).isEqualTo(authResult.accessToken());

    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenMissingException quando o token for nulo")
  void execute_shouldThrowRefreshTokenMissingException_whenTokenIsNull() {
    // Act
    assertThatThrownBy(() -> refreshTokenUseCase.execute(null))
        .isInstanceOf(InvalidTokenFormatException.class)
        .satisfies(
            ex -> {
              InvalidTokenFormatException exception = (InvalidTokenFormatException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REQUIRED);
            });

    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName(
      "Deve lançar RefreshTokenNotFoundException quando o refresh token não for encontrado")
  void execute_shouldThrowRefreshTokenNotFoundException_whenTokenIsMissing() {
    // Arrange
    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
        .isInstanceOf(RefreshTokenNotFoundException.class)
        .satisfies(
            ex -> {
              RefreshTokenNotFoundException exception = (RefreshTokenNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
            });

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenExpiredException quando o token estiver expirado")
  void execute_shouldThrowRefreshTokenExpiredException_whenTokenExpired() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    RefreshToken expiredToken = TestDataProvider.createExpiredRefreshToken(user);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(expiredToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
        .isInstanceOf(RefreshTokenExpiredException.class)
        .satisfies(
            ex -> {
              RefreshTokenExpiredException exception = (RefreshTokenExpiredException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);
            });

    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, times(1)).delete(expiredToken);
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
  @DisplayName("Deve lançar AccountStatusForbiddenException para status inválidos")
  void execute_shouldThrowAccountStatusForbiddenException_forInvalidAccountStatuses(
      AccountStatus invalidStatus, ErrorCode expectedErrorCode) {
    // Arrange
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();
    RefreshToken refreshToken = TestDataProvider.createRefreshToken(user);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
              assertThat(exception.getMessage()).isEqualTo(expectedErrorCode.getMessage());
            });

    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }
}

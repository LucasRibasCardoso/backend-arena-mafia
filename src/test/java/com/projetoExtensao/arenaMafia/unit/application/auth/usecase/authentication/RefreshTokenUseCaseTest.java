package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.RefreshTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenMissingException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "hashedPassword",
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve renovar os tokens com sucesso quando for enviado um refresh token válido")
  void execute_shouldReturnTokens() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    RefreshToken refreshToken = RefreshToken.create(7L, user);
    AuthResult expectedResponse = new AuthResult(user, "access-token", refreshTokenVO);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));
    when(authPort.generateTokens(user)).thenReturn(expectedResponse);

    // Act
    AuthResult response = refreshTokenUseCase.execute(refreshTokenVO);

    // Assert
    assertThat(response.user()).isEqualTo(expectedResponse.user());
    assertThat(response.refreshToken()).isEqualTo(expectedResponse.refreshToken());
    assertThat(response.accessToken()).isEqualTo(expectedResponse.accessToken());

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, times(1)).generateTokens(user);
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenMissingException quando o token for nulo")
  void execute_shouldThrowRefreshTokenMissingException_whenTokenIsNull() {
    // Act
    assertThatThrownBy(() -> refreshTokenUseCase.execute(null))
        .isInstanceOf(RefreshTokenMissingException.class)
        .hasMessage("Sua sessão expirou. Por favor, faça login novamente.");

    verify(refreshTokenRepository, never()).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenNotFoundException quando o token não for encontrado")
  void execute_shouldThrowRefreshTokenNotFoundException_whenTokenIsMissing() {
    // Arrange
    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
        .isInstanceOf(RefreshTokenNotFoundException.class)
        .hasMessage("Refresh token não encontrado.");

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenExpiredException quando o token estiver expirado")
  void execute_shouldThrowRefreshTokenExpiredException_whenTokenExpired() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    RefreshToken expiredToken = RefreshToken.create(-1L, user);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(expiredToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
        .isInstanceOf(RefreshTokenExpiredException.class)
        .hasMessage("Sua sessão expirou. Por favor, faça login novamente.");

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(refreshTokenRepository, times(1)).delete(expiredToken);
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Nested
  @DisplayName("Deve lançar exceção quando a conta não está ativada")
  class AccountStateTests {

    @Test
    @DisplayName("Deve lançar AccountStateConflictException quando a conta está bloqueada")
    void execute_shouldThrowAccountStateConflictException_whenUserAccountIsLocked() {
      // Arrange
      User lockedUser = createUser(AccountStatus.LOCKED);
      RefreshToken refreshToken = RefreshToken.create(7L, lockedUser);

      when(refreshTokenRepository.findByToken(refreshTokenVO))
          .thenReturn(Optional.of(refreshToken));

      // Act & Assert
      assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

      verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
      verify(authPort, never()).generateTokens(any(User.class));
      verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName(
        "Deve lançar AccountStateConflictException quando a conta está pendente de verificação")
    void execute_shouldThrowAccountStateConflictException_whenUserAccountIsPendingVerification() {
      // Arrange
      User pendingUser = createUser(AccountStatus.PENDING_VERIFICATION);
      RefreshToken refreshToken = RefreshToken.create(7L, pendingUser);

      when(refreshTokenRepository.findByToken(refreshTokenVO))
          .thenReturn(Optional.of(refreshToken));

      // Act & Assert
      assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

      verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
      verify(authPort, never()).generateTokens(any(User.class));
      verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve lançar AccountStateConflictException quando a conta está desativada")
    void execute_shouldThrowAccountStateConflictException_whenUserAccountIsDisabled() {
      // Arrange
      User disabledUser = createUser(AccountStatus.DISABLED);
      RefreshToken refreshToken = RefreshToken.create(7L, disabledUser);

      when(refreshTokenRepository.findByToken(refreshTokenVO))
          .thenReturn(Optional.of(refreshToken));

      // Act & Assert
      assertThatThrownBy(() -> refreshTokenUseCase.execute(refreshTokenVO))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");

      verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
      verify(authPort, never()).generateTokens(any(User.class));
      verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }
  }
}

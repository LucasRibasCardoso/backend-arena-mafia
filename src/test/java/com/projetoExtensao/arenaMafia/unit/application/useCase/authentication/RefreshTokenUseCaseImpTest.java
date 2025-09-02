package com.projetoExtensao.arenaMafia.unit.application.useCase.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.RefreshTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.RefreshTokenInvalidFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.RefreshTokenRequestDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para RefreshTokenUseCase")
public class RefreshTokenUseCaseImpTest {

  @Mock private AuthPort authPort;
  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;

  @InjectMocks private RefreshTokenUseCaseImp refreshTokenUseCase;

  private User createUser(AccountStatus accountStatus) {
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "hashedPassword",
        accountStatus,
        RoleEnum.ROLE_USER,
        Instant.now());
  }

  @Test
  @DisplayName("Deve renovar os tokens com sucesso quando for enviado um refresh token válido")
  void execute_shouldReturnTokens() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    RefreshToken refreshToken = RefreshToken.create(7L, user);
    String token = UUID.randomUUID().toString();

    AuthResult expectedResponse = new AuthResult(user, "access-token", "refresh-token");
    var request = new RefreshTokenRequestDto(token);

    when(refreshTokenRepository.findByToken(RefreshTokenVO.fromString(token)))
        .thenReturn(Optional.of(refreshToken));
    when(authPort.generateTokens(user)).thenReturn(expectedResponse);

    // Act
    AuthResult response = refreshTokenUseCase.execute(request);

    // Assert
    assertThat(response.user()).isEqualTo(expectedResponse.user());
    assertThat(response.refreshToken()).isEqualTo(expectedResponse.refreshToken());
    assertThat(response.accessToken()).isEqualTo(expectedResponse.accessToken());

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, times(1)).generateTokens(user);
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Deve lançar RefreshTokenNotFoundException quando o token for nulo ou vazio")
  void execute_shouldThrowRefreshTokenNotFoundException_whenTokenIsNullOrEmpty(String token) {
    // Arrange
    var request = new RefreshTokenRequestDto(token);

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(request))
        .isInstanceOf(RefreshTokenNotFoundException.class)
        .hasMessage("Sua sessão expirou. Por favor, faça login novamente.");

    verify(refreshTokenRepository, never()).findByToken(any(RefreshTokenVO.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenNotFoundException quando o token não for encontrado")
  void execute_shouldThrowRefreshTokenNotFoundException_whenTokenIsMissing() {
    // Arrange
    String token = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(token);
    var request = new RefreshTokenRequestDto(token);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(request))
        .isInstanceOf(RefreshTokenNotFoundException.class)
        .hasMessage("Refresh token não encontrado.");

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName(
      "Deve lançar RefreshTokenInvalidFormatException quando o token tiver formato inválido")
  void execute_shouldThrowRefreshTokenInvalidFormatException_whenTokenFormatInvalid() {
    // Arrange
    var invalidRequestDto = new RefreshTokenRequestDto("invalid-token");

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(invalidRequestDto))
        .isInstanceOf(RefreshTokenInvalidFormatException.class)
        .hasMessage("Formato inválido para o refresh token.");

    verify(refreshTokenRepository, never()).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName("Deve lançar RefreshTokenExpiredException quando o token estiver expirado")
  void execute_shouldThrowRefreshTokenExpiredException_whenTokenExpired() {
    // Arrange
    String token = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(token);
    User user = createUser(AccountStatus.ACTIVE);
    RefreshToken expiredRefreshToken = RefreshToken.create(-1L, user);
    var request = new RefreshTokenRequestDto(token);

    when(refreshTokenRepository.findByToken(refreshTokenVO))
        .thenReturn(Optional.of(expiredRefreshToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(request))
        .isInstanceOf(RefreshTokenExpiredException.class)
        .hasMessage("Sua sessão expirou. Por favor, faça login novamente.");

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(refreshTokenRepository, times(1)).delete(expiredRefreshToken);
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver bloqueada")
  void execute_shouldThrowAccountStateConflictException_whenUserAccountIsLocked() {
    // Arrange
    String token = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(token);
    User lockedUser = createUser(AccountStatus.LOCKED);
    RefreshToken refreshToken = RefreshToken.create(7L, lockedUser);
    var request = new RefreshTokenRequestDto(token);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver pendente de verificação")
  void execute_shouldThrowAccountStateConflictException_whenUserAccountIsPendingVerification() {
    // Arrange
    String token = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(token);
    User pendingUser = createUser(AccountStatus.PENDING_VERIFICATION);
    RefreshToken refreshToken = RefreshToken.create(7L, pendingUser);
    var request = new RefreshTokenRequestDto(token);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver desativada")
  void execute_shouldThrowAccountStateConflictException_whenUserAccountIsDisabled() {
    // Arrange
    String token = UUID.randomUUID().toString();
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(token);
    User disabledUser = createUser(AccountStatus.DISABLED);
    RefreshToken refreshToken = RefreshToken.create(7L, disabledUser);
    var request = new RefreshTokenRequestDto(token);

    when(refreshTokenRepository.findByToken(refreshTokenVO)).thenReturn(Optional.of(refreshToken));

    // Act & Assert
    assertThatThrownBy(() -> refreshTokenUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está desativada.");

    verify(refreshTokenRepository, times(1)).findByToken(any(RefreshTokenVO.class));
    verify(authPort, never()).generateTokens(any(User.class));
    verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
  }
}

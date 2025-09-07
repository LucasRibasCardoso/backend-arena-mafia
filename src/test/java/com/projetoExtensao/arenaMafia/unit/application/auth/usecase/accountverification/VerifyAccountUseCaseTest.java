package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.accountverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp.VerifyAccountUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para VerifyAccountUseCase")
public class VerifyAccountUseCaseTest {

  @Mock private OtpPort otpPort;
  @Mock private AuthPort authPort;
  @Mock private OtpSessionPort otpSessionPort;
  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private VerifyAccountUseCaseImp verifyAccountUseCase;

  private final String defaultOtp = "123456";

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "123456",
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve verificar e ativar a conta do usuário com sucesso")
  void execute_shouldCheckAndActivateUserAccount_successfully() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    UUID userId = user.getId();
    String defaultAccessToken = "access_token";
    String defaultRefreshToken = "refresh_token";
    var authResult = new AuthResult(user, defaultAccessToken, defaultRefreshToken);

    String otpSessionId = UUID.randomUUID().toString();
    var request = new ValidateOtpRequestDto(otpSessionId, defaultOtp);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(authPort.generateTokens(user)).thenReturn(authResult);

    // Act
    AuthResult response = verifyAccountUseCase.execute(request);

    // Assert
    assertThat(response.user()).isEqualTo(user);
    assertThat(response.accessToken()).isEqualTo(defaultAccessToken);
    assertThat(response.refreshToken()).isEqualTo(defaultRefreshToken);

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(user.getId(), defaultOtp);
    verify(userRepository, times(1)).save(user);
    verify(authPort, times(1)).generateTokens(user);
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();

    String otpSessionId = UUID.randomUUID().toString();
    var request = new ValidateOtpRequestDto(otpSessionId, defaultOtp);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage(
            "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta.");

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando o otpSessionId for inválido")
  void execute_shouldThrowException_whenOtpSessionIdIsInvalid() {
    // Arrange
    String userId = "invalid-uuid";
    var request = new ValidateOtpRequestDto(userId, defaultOtp);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage("Sessão de verificação inválida ou expirada.");

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(userId);
    verify(userRepository, never()).findById(any(UUID.class));
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException para um código de verificação inválido")
  void execute_shouldThrowInvalidOtpException_forInvalidOtpCode() {
    // Arrange
    String errorMessage = "Código de verificação inválido ou expirado.";
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    UUID userId = user.getId();

    String otpSessionId = UUID.randomUUID().toString();
    String invalidOtp = "aaabbb";
    var request = new ValidateOtpRequestDto(otpSessionId, invalidOtp);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    doThrow(new InvalidOtpException(errorMessage))
        .when(otpPort)
        .validateOtp(user.getId(), invalidOtp);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage(errorMessage);

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(user.getId(), invalidOtp);
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção ao tentar verificar uma conta que já estiver ativa")
  void execute_shouldThrowException_whenAccountIsNotPendingVerification() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    UUID userId = user.getId();

    String otpSessionId = UUID.randomUUID().toString();
    var request = new ValidateOtpRequestDto(otpSessionId, defaultOtp);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Não é possível ativar uma conta que não está pendente de verificação.");

    // Verify
    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(user.getId(), defaultOtp);
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }
}

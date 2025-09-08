package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.ValidatePasswordResetOtpUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
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
@DisplayName("Testes unitários para GeneratePasswordResetToken")
public class ValidatePasswordResetOtpUseCaseTest {

  @Mock private OtpPort otpPort;
  @Mock private OtpSessionPort otpSessionPort;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PasswordResetTokenPort passwordResetTokenPort;

  @InjectMocks private ValidatePasswordResetOtpUseCaseImp generatePasswordResetTokenUseCase;

  private final OtpCode otpCode = OtpCode.generate();
  private final OtpSessionId otpSessionId = OtpSessionId.generate();
  private final ResetToken resetToken = ResetToken.generate();

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
  @DisplayName("Deve gerar e retornar um token de redefinição de senha para uma sessão OTP válida")
  void execute_shouldGenerateAndReturnResetToken_forValidRequest() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    UUID userId = user.getId();

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordResetTokenPort.generateToken(userId)).thenReturn(resetToken);

    // Act
    PasswordResetTokenResponseDto response = generatePasswordResetTokenUseCase.execute(request);

    // Assert
    assertThat(response.passwordResetToken()).isEqualTo(resetToken);

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(userId, otpCode);
    verify(passwordResetTokenPort, times(1)).generateToken(userId);
  }

  @Test
  @DisplayName("Deve lançar exceção quando o ID da sessão OTP for inválido")
  void execute_shouldThrowInvalidOtpException_whenOtpSessionIdIsInvalid() {
    // Arrange
    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage("Sessão de verificação inválida ou expirada.");

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, never()).findById(any(UUID.class));
    verify(otpPort, never()).validateOtp(any(UUID.class), any(OtpCode.class));
    verify(passwordResetTokenPort, never()).generateToken(any(UUID.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserIsNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("Usuário não encontrado.");

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, never()).validateOtp(any(UUID.class), any(OtpCode.class));
    verify(passwordResetTokenPort, never()).generateToken(any(UUID.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o código OTP for inválido")
  void execute_shouldThrowException_whenOtpIsInvalid() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    UUID userId = user.getId();

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    String errorMessage = "Código de verificação inválido ou expirado.";
    doThrow(new InvalidOtpException(errorMessage)).when(otpPort).validateOtp(user.getId(), otpCode);

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage(errorMessage);

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(userId, otpCode);
    verify(passwordResetTokenPort, never()).generateToken(any(UUID.class));
  }

  @Nested
  @DisplayName("Deve lançar exceção quando a conta não está ativada")
  class AccountStateTests {
    @Test
    @DisplayName("Deve lançar exceção quando a conta está bloqueada")
    void execute_shouldThrowAccountStateConflictException_whenAccountIsLocked() {
      // Arrange
      User user = createUser(AccountStatus.LOCKED);
      UUID userId = user.getId();

      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

      when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

      verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
      verify(userRepository, times(1)).findById(userId);
      verify(otpPort, never()).validateOtp(any(UUID.class), any(OtpCode.class));
      verify(passwordResetTokenPort, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando a conta está pendente de verificação")
    void execute_shouldThrowAccountStateConflictException_whenAccountIsPendingVerification() {
      // Arrange
      User user = createUser(AccountStatus.PENDING_VERIFICATION);
      UUID userId = user.getId();

      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

      when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

      verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
      verify(userRepository, times(1)).findById(userId);
      verify(otpPort, never()).validateOtp(any(UUID.class), any(OtpCode.class));
      verify(passwordResetTokenPort, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando a conta está desativada")
    void execute_shouldThrowAccountStateConflictException_whenAccountIsDisabled() {
      // Arrange
      User user = createUser(AccountStatus.DISABLED);
      UUID userId = user.getId();

      var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

      when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");

      verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
      verify(userRepository, times(1)).findById(userId);
      verify(otpPort, never()).validateOtp(any(UUID.class), any(OtpCode.class));
      verify(passwordResetTokenPort, never()).generateToken(any());
    }
  }
}

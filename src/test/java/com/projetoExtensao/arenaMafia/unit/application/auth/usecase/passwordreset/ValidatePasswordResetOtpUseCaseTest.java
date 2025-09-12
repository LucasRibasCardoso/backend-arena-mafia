package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.ValidatePasswordResetOtpUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpSessionException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.PasswordResetTokenResponseDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

  @Test
  @DisplayName("Deve gerar e retornar um token de redefinição de senha para uma sessão OTP válida")
  void execute_shouldGenerateAndReturnResetToken_forValidRequest() {
    // Arrange
    OtpCode otpCode = OtpCode.generate();
    OtpSessionId otpSessionId = OtpSessionId.generate();
    ResetToken resetToken = ResetToken.generate();

    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordResetTokenPort.generateToken(userId)).thenReturn(resetToken);

    // Act
    PasswordResetTokenResponseDto response = generatePasswordResetTokenUseCase.execute(request);

    // Assert
    assertThat(response.passwordResetToken()).isEqualTo(resetToken);

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
        .isInstanceOf(InvalidOtpSessionException.class)
        .satisfies(
            ex -> {
              InvalidOtpSessionException exception = (InvalidOtpSessionException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.OTP_SESSION_INVALID_OR_EXPIRED);
            });

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
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(passwordResetTokenPort, never()).generateToken(any(UUID.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o código OTP for inválido")
  void execute_shouldThrowException_whenOtpIsInvalid() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    doThrow(new InvalidOtpException(ErrorCode.OTP_CODE_INVALID_OR_EXPIRED))
        .when(otpPort)
        .validateOtp(user.getId(), otpCode);

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(InvalidOtpException.class)
        .satisfies(
            ex -> {
              InvalidOtpException exception = (InvalidOtpException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OTP_CODE_INVALID_OR_EXPIRED);
            });

    verify(passwordResetTokenPort, never()).generateToken(any(UUID.class));
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
  @DisplayName("Deve lançar AccountStatusForbiddenException quando a conta não está ativa")
  void execute_shouldThrowAccountStatusForbiddenException_forNonActiveAccountStatuses(
      AccountStatus invalidStatus, ErrorCode expectedError) {
    // Arrange
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();
    UUID userId = user.getId();

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedError);
            });

    verify(otpPort, never()).validateOtp(any(UUID.class), any(OtpCode.class));
    verify(passwordResetTokenPort, never()).generateToken(any(UUID.class));
  }
}

package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.otp.imp.ResendOtpUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpSessionException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ResendOtpUseCase")
public class ResendOtpUseCaseTest {

  @Mock private OtpSessionPort otpSessionPort;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private UserRepositoryPort userRepositoryPort;
  @InjectMocks private ResendOtpUseCaseImp resendOtpUseCaseTest;

  private final OtpSessionId otpSessionId = OtpSessionId.generate();

  @ParameterizedTest
  @EnumSource(
      value = AccountStatus.class,
      names = {"ACTIVE", "PENDING_VERIFICATION"})
  @DisplayName("Deve reenviar o código OTP com sucesso quando a conta está ativada ou pendente")
  void shouldResendOtpSuccessfully(AccountStatus status) {
    // Arrange
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(status).build();
    UUID userId = user.getId();

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

    // Act
    resendOtpUseCaseTest.execute(otpSessionId);

    // Assert
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpSessionException quando a sessão OTP for inválida")
  void shouldThrowInvalidOtpSessionException_whenOtpSessionIsInvalid() {
    // Arrange
    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resendOtpUseCaseTest.execute(otpSessionId))
        .isInstanceOf(InvalidOtpSessionException.class)
        .satisfies(
            ex -> {
              InvalidOtpSessionException exception = (InvalidOtpSessionException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.OTP_SESSION_ID_INCORRECT_OR_EXPIRED);
            });

    verify(userRepositoryPort, never()).findById(any());
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void shouldThrowException_whenUserNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resendOtpUseCaseTest.execute(otpSessionId))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.application.auth.usecase.otp.ResendOtpUseCaseTest#accountStatusNonActiveOrPending")
  @DisplayName("Deve lançar AccountStatusForbiddenException quando o status da conta é inválido")
  void shouldThrowException_whenInactiveOrBlockedAccount(
      AccountStatus invalidStatus, ErrorCode expectedErrorCode) {
    // Arrange
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();
    UUID userId = user.getId();

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> resendOtpUseCaseTest.execute(otpSessionId))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
            });
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  private static Stream<Arguments> accountStatusNonActiveOrPending() {
    return Stream.of(
        arguments(AccountStatus.LOCKED, ErrorCode.ACCOUNT_LOCKED),
        arguments(AccountStatus.DISABLED, ErrorCode.ACCOUNT_DISABLED));
  }
}

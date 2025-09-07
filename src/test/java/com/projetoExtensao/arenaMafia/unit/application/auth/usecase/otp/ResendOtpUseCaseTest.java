package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.otp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.otp.imp.ResendOtpUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

  private User createUser(AccountStatus status) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "hashedPassword",
        status,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @ParameterizedTest
  @EnumSource(
      value = AccountStatus.class,
      names = {"ACTIVE", "PENDING_VERIFICATION"})
  @DisplayName("Deve reenviar o código OTP com sucesso quando a conta está ativada ou pendente")
  void shouldResendOtpSuccessfully(AccountStatus status) {
    // Arrange
    User user = createUser(status);
    UUID userId = user.getId();
    String otpSessionId = UUID.randomUUID().toString();

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

    // Act
    resendOtpUseCaseTest.execute(otpSessionId);

    // Assert
    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepositoryPort, times(1)).findById(userId);
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando a sessão OTP for inválida")
  void shouldThrowExceptionForInvalidOtpSession() {
    // Arrange
    String otpSessionId = UUID.randomUUID().toString();
    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resendOtpUseCaseTest.execute(otpSessionId))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage("Sessão de verificação inválida ou expirada.");

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepositoryPort, never()).findById(any());
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void shouldThrowException_whenUserNotFound() {
    // Arrange
    String otpSessionId = UUID.randomUUID().toString();
    UUID userId = UUID.randomUUID();

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resendOtpUseCaseTest.execute(otpSessionId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("Usuário não encontrado.");

    verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
    verify(userRepositoryPort, times(1)).findById(userId);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Nested
  @DisplayName("Deve lançar exceção quando a conta não está ativa ou pendente")
  class AccountStateConflictTests {

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"DISABLED", "LOCKED"})
    @DisplayName("Deve lançar exceção quando a conta está desativada ou bloqueada")
    void shouldThrowException_whenInactiveOrBlockedAccount(AccountStatus status) {
      // Arrange
      User user = createUser(status);
      UUID userId = user.getId();
      String otpSessionId = UUID.randomUUID().toString();

      when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
      when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> resendOtpUseCaseTest.execute(otpSessionId))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Sua conta está bloqueada ou desativada. Por favor, contate o suporte.");

      verify(otpSessionPort, times(1)).findUserIdByOtpSessionId(otpSessionId);
      verify(userRepositoryPort, times(1)).findById(userId);
      verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
    }
  }
}

package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp.ResendChangePhoneOtpUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PhoneChangeNotInitiatedException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ResendChangePhoneOtpUseCase")
public class ResendChangePhoneOtpUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private PendingPhoneChangePort pendingPhoneChangePort;
  @InjectMocks private ResendChangePhoneOtpUseCaseImp resendChangePhoneOtpUseCase;

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+5511988887777",
        "hashedPassword",
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve publicar um evento para reenviar o OTP de alteração de telefone")
  void execute_shouldPublishEventToResendOtp() {
    // Arrange
    User mockUser = createUser(AccountStatus.ACTIVE);
    UUID userId = mockUser.getId();
    String newPhone = "+5511999999999";

    when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.of(newPhone));
    when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

    // Act
    resendChangePhoneOtpUseCase.execute(userId);

    // Assert
    verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(userId);
    verify(userRepository, times(1)).findById(userId);
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando não houver alteração de telefone pendente")
  void execute_shouldThrowExceptionWhenNoPendingPhoneChange() {
    // Arrange
    UUID userId = UUID.randomUUID();

    when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.empty());

    // Act
    assertThatThrownBy(() -> resendChangePhoneOtpUseCase.execute(userId))
        .isInstanceOf(PhoneChangeNotInitiatedException.class)
        .hasMessageContaining("Nenhuma alteração de telefone pendente encontrada para o usuário.");

    // Assert
    verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(userId);
    verify(userRepository, never()).findById(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Nested
  @DisplayName("Deve lançar exceção quando a conta não está ativa")
  class AccountStatusTests {

    @Test
    @DisplayName(
        "Deve lançar AccountStateConflictException quando a conta está pedente de verificação")
    void execute_shouldThrowExceptionWhenAccountPendingVerification() {
      // Arrange
      User mockUser = createUser(AccountStatus.PENDING_VERIFICATION);
      UUID userId = mockUser.getId();
      String newPhone = "+5511999999999";

      when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.of(newPhone));
      when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

      // Act
      assertThatThrownBy(() -> resendChangePhoneOtpUseCase.execute(userId))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

      // Assert
      verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(userId);
      verify(userRepository, times(1)).findById(userId);
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve lançar AccountStateConflictException quando a conta está bloqueada")
    void execute_shouldThrowExceptionWhenAccountLocked() {
      // Arrange
      User mockUser = createUser(AccountStatus.LOCKED);
      UUID userId = mockUser.getId();
      String newPhone = "+5511999999999";

      when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.of(newPhone));
      when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

      // Act
      assertThatThrownBy(() -> resendChangePhoneOtpUseCase.execute(userId))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

      // Assert
      verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(userId);
      verify(userRepository, times(1)).findById(userId);
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve lançar AccountStateConflictException quando a conta está desativada")
    void execute_shouldThrowExceptionWhenAccountDisabled() {
      // Arrange
      User mockUser = createUser(AccountStatus.DISABLED);
      UUID userId = mockUser.getId();
      String newPhone = "+5511999999999";

      when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.of(newPhone));
      when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

      // Act
      assertThatThrownBy(() -> resendChangePhoneOtpUseCase.execute(userId))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");

      // Assert
      verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(userId);
      verify(userRepository, times(1)).findById(userId);
      verify(eventPublisher, never()).publishEvent(any());
    }
  }
}

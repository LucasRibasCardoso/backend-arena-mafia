package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp.ResendChangePhoneOtpUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PhoneChangeNotInitiatedException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ResendChangePhoneOtpUseCase")
public class ResendChangePhoneOtpUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private PendingPhoneChangePort pendingPhoneChangePort;
  @InjectMocks private ResendChangePhoneOtpUseCaseImp resendChangePhoneOtpUseCase;

  @Test
  @DisplayName("Deve publicar um evento para reenviar o OTP de alteração de telefone")
  void execute_shouldPublishEventToResendOtp() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();
    String newPhone = "+5511999999999";

    when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.of(newPhone));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act
    resendChangePhoneOtpUseCase.execute(userId);

    // Assert
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName(
      "Deve lançar PhoneChangeNotInitiatedException quando não houver alteração de telefone pendente")
  void execute_shouldThrowException_whenNoPendingPhoneChange() {
    // Arrange
    UUID userId = UUID.randomUUID();

    when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.empty());

    // Act
    assertThatThrownBy(() -> resendChangePhoneOtpUseCase.execute(userId))
        .isInstanceOf(PhoneChangeNotInitiatedException.class)
        .satisfies(
            ex -> {
              PhoneChangeNotInitiatedException exception = (PhoneChangeNotInitiatedException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_CHANGE_NOT_INITIATED);
            });

    // Assert
    verify(eventPublisher, never()).publishEvent(any());
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
  @DisplayName("Deve lançar AccountStatusForbiddenException quando a conta não está ativa")
  void execute_shouldThrowException_whenAccountNotActive(
      AccountStatus status, ErrorCode errorCode) {
    // Arrange
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(status).build();
    UUID userId = user.getId();
    String newPhone = "+5511999999999";

    when(pendingPhoneChangePort.findPhoneByUserId(userId)).thenReturn(Optional.of(newPhone));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act
    assertThatThrownBy(() -> resendChangePhoneOtpUseCase.execute(userId))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            });

    // Assert
    verify(eventPublisher, never()).publishEvent(any());
  }
}

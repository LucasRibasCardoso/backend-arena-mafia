package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.accountverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp.ResendCodeUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ResendCodeUseCase")
public class ResendCodeUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ResendCodeUseCaseImp resendCodeUseCase;

  private final String defaultPhone = "+558320548181";
  private final String unformattedPhone = "+558320548181";
  private final String formattedPhone = defaultPhone;

  private User createUser(AccountStatus status) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        defaultPhone,
        "hashedpassword",
        status,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName(
      "Deve disparar um evento de verificação quando o usuário for encontrado e estiver pendente")
  void execute_shouldPublishEvent_whenUserExists() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    var request = new ResendCodeRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act
    resendCodeUseCase.execute(request);

    // Assert
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);

    ArgumentCaptor<OnVerificationRequiredEvent> eventCaptor =
        ArgumentCaptor.forClass(OnVerificationRequiredEvent.class);

    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    OnVerificationRequiredEvent publishedEvent = eventCaptor.getValue();

    assertThat(publishedEvent.user()).isEqualTo(user);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
  }

  @Test
  @DisplayName("Não deve disparar um evento de verificação quando um usuário não for encontrado")
  void execute_shouldDoNothing_whenUserIsNotFound() {
    // Arrange
    var request = new ResendCodeRequestDto(unformattedPhone);
    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.empty());

    // Act
    assertDoesNotThrow(() -> resendCodeUseCase.execute(request));

    // Verify
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Deve formatar o número de telefone antes de buscar o usuário")
  void execute_shouldFormatPhoneNumber_beforeSearchingUser() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    var request = new ResendCodeRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act
    resendCodeUseCase.execute(request);

    // Verify
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando o número de telefone for inválido")
  void execute_shouldThrowException_whenPhoneNumberIsInvalid() {
    // Arrange
    String errorMessage = "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.";
    String invalidPhone = "123456789";
    var request = new ResendCodeRequestDto(invalidPhone);

    when(phoneValidator.formatToE164(invalidPhone))
        .thenThrow(new BadPhoneNumberException(errorMessage));

    // Act & Assert
    assertThatThrownBy(() -> resendCodeUseCase.execute(request))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage(errorMessage);

    verify(phoneValidator, times(1)).formatToE164(invalidPhone);
    verify(userRepository, never()).findByPhone(anyString());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta não estiver pendente de verificação")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsNotPending() {
    // Arrange
    User activeUser = createUser(AccountStatus.ACTIVE);
    var request = new ResendCodeRequestDto(defaultPhone);

    when(phoneValidator.formatToE164(request.phone())).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(activeUser));

    // Act & Assert
    assertThatThrownBy(() -> resendCodeUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Só é possível reenviar o código para contas pendentes de verificação.");

    // Verify
    verify(userRepository, times(1)).findByPhone(request.phone());
    verify(eventPublisher, never()).publishEvent(any());
  }
}

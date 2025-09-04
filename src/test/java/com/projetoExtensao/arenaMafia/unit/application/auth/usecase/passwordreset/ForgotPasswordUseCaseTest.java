package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.ForgotPasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
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
@DisplayName("Testes unitários para ForgotPasswordUseCase")
public class ForgotPasswordUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ForgotPasswordUseCaseImp forgotPasswordUseCase;

  private final String defaultPhone = "+558320548181";
  private final String unformattedPhone = "+558320548181";
  private final String formattedPhone = defaultPhone;

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
  @DisplayName(
      "Deve publicar um evento para envio de sms quando o usuário for encontrado pelo telefone")
  void execute_shouldPublishEvent_whenUserIsFoundByPhone() {
    // Arrange
    var requestDto = new ForgotPasswordRequestDto(unformattedPhone);
    User user = createUser(AccountStatus.ACTIVE);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act
    forgotPasswordUseCase.execute(requestDto);

    // Assert
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    ArgumentCaptor<OnVerificationRequiredEvent> eventCaptor =
        ArgumentCaptor.forClass(OnVerificationRequiredEvent.class);

    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    OnVerificationRequiredEvent publishedEvent = eventCaptor.getValue();

    assertThat(publishedEvent.user()).isEqualTo(user);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
  }

  @Test
  @DisplayName("Não deve publicar um evento quando o usuário não for encontrado")
  void execute_shouldDoNothing_whenUserIsNotFound() {
    // Arrange
    var requestDto = new ForgotPasswordRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.empty());

    // Act
    forgotPasswordUseCase.execute(requestDto);

    // Assert
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Deve propagar BadPhoneNumberException para um formato de telefone inválido")
  void execute_shouldPropagateException_whenPhoneFormatIsInvalid() {
    // Arrange
    String errorMessage = "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.";
    String invalidPhone = "123456789";
    var requestDto = new ForgotPasswordRequestDto(invalidPhone);

    when(phoneValidator.formatToE164(invalidPhone))
        .thenThrow(new BadPhoneNumberException(errorMessage));

    // Act & Assert
    assertThatThrownBy(() -> forgotPasswordUseCase.execute(requestDto))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage(errorMessage);

    // Verify
    verify(phoneValidator, times(1)).formatToE164(invalidPhone);
    verify(userRepository, never()).findByPhone(anyString());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver bloqueada")
  void execute_shouldThrowException_whenUserAccountIsBlocked() {
    // Arrange
    User user = createUser(AccountStatus.LOCKED);
    var requestDto = new ForgotPasswordRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> forgotPasswordUseCase.execute(requestDto))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

    // Verify
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver pendente de verificação")
  void execute_shouldThrowException_whenUserAccountIsPending() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    var requestDto = new ForgotPasswordRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> forgotPasswordUseCase.execute(requestDto))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

    // Verify
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta do usuário estiver desativada")
  void execute_shouldThrowException_whenUserAccountIsDisabled() {
    // Arrange
    User user = createUser(AccountStatus.DISABLED);
    var requestDto = new ForgotPasswordRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> forgotPasswordUseCase.execute(requestDto))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está desativada.");

    // Verify
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(eventPublisher, never()).publishEvent(any());
  }
}

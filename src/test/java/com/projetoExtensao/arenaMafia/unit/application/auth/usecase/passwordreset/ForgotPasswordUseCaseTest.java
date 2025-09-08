package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.ForgotPasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

  @Mock private OtpSessionPort otpSessionPort;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private ForgotPasswordUseCaseImp forgotPasswordUseCase;

  private final String defaultPhone = "+558320548181";
  private final String unformattedPhone = "+558320548181";
  private final String formattedPhone = defaultPhone;

  private final OtpSessionId otpSessionId = OtpSessionId.generate();

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        formattedPhone,
        "hashedPassword",
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve publicar um evento para envio de sms quando o usuário for encontrado")
  void execute_shouldPublishEvent_whenUserIsFoundByPhone() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    var request = new ForgotPasswordRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));
    when(otpSessionPort.generateOtpSession(user.getId())).thenReturn(otpSessionId);

    // Act
    ForgotPasswordResponseDto response = forgotPasswordUseCase.execute(request);

    // Assert
    assertThat(response.otpSessionId()).isEqualTo(otpSessionId);
    assertThat(response.message())
        .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpSessionPort, times(1)).generateOtpSession(user.getId());
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));

    ArgumentCaptor<OnVerificationRequiredEvent> eventCaptor =
        ArgumentCaptor.forClass(OnVerificationRequiredEvent.class);

    verify(eventPublisher).publishEvent(eventCaptor.capture());
    User publishedUser = eventCaptor.getValue().getUser();
    assertThat(publishedUser).isEqualTo(user);
  }

  @Test
  @DisplayName("Não deve publicar um evento quando o usuário não for encontrado")
  void execute_shouldDoNothing_whenUserIsNotFound() {
    // Arrange
    var request = new ForgotPasswordRequestDto(unformattedPhone);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.empty());

    // Act
    ForgotPasswordResponseDto response = forgotPasswordUseCase.execute(request);

    // Assert
    assertThat(response.otpSessionId().toString()).hasSize(36); // UUID Fake
    assertThat(response.message())
        .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o formato do telefone for inválido")
  void execute_shouldThrowException_whenPhoneFormatIsInvalid() {
    // Arrange
    String invalidPhone = "123456789";
    var requestDto = new ForgotPasswordRequestDto(invalidPhone);

    String errorMessage = "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.";
    doThrow(new BadPhoneNumberException(errorMessage))
        .when(phoneValidator)
        .formatToE164(invalidPhone);

    // Act & Assert
    assertThatThrownBy(() -> forgotPasswordUseCase.execute(requestDto))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage(errorMessage);

    // Verify
    verify(phoneValidator, times(1)).formatToE164(invalidPhone);
    verify(userRepository, never()).findByPhone(anyString());
    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Nested
  @DisplayName("Exceções lançadas quando a conta do usuário não está ativa")
  class AccountStatusTests {

    @Test
    @DisplayName("Deve lançar exceção quando a conta está bloqueada")
    void execute_shouldThrowException_whenAccountIsBlocked() {
      // Arrange
      User user = createUser(AccountStatus.LOCKED);
      var request = new ForgotPasswordRequestDto(unformattedPhone);

      when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
      when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> forgotPasswordUseCase.execute(request))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

      verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
      verify(userRepository, times(1)).findByPhone(formattedPhone);
      verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
      verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando a conta está pendente de verificação")
    void execute_shouldThrowException_whenAccountIsPending() {
      // Arrange
      User user = createUser(AccountStatus.PENDING_VERIFICATION);
      var request = new ForgotPasswordRequestDto(unformattedPhone);

      when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
      when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> forgotPasswordUseCase.execute(request))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

      verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
      verify(userRepository, times(1)).findByPhone(formattedPhone);
      verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
      verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando a conta está desativada")
    void execute_shouldThrowException_whenAccountIsDisabled() {
      // Arrange
      User user = createUser(AccountStatus.DISABLED);
      var request = new ForgotPasswordRequestDto(unformattedPhone);

      when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
      when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

      // Act & Assert
      assertThatThrownBy(() -> forgotPasswordUseCase.execute(request))
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");

      // Verify
      verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
      verify(userRepository, times(1)).findByPhone(formattedPhone);
      verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
      verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    }
  }
}

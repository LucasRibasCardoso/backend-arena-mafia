package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.ForgotPasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ForgotPasswordRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.ForgotPasswordResponseDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

  @Test
  @DisplayName("Deve publicar um evento para envio de sms quando o usuário for encontrado")
  void execute_shouldPublishEvent_whenUserIsFoundByPhone() {
    // Arrange
    OtpSessionId otpSessionId = OtpSessionId.generate();
    User user = TestDataProvider.createActiveUser();
    String defaultPhone = user.getPhone();

    var request = new ForgotPasswordRequestDto(defaultPhone);

    when(phoneValidator.formatToE164(defaultPhone)).thenReturn(defaultPhone);
    when(userRepository.findByPhone(defaultPhone)).thenReturn(Optional.of(user));
    when(otpSessionPort.generateOtpSession(user.getId())).thenReturn(otpSessionId);

    // Act
    ForgotPasswordResponseDto response = forgotPasswordUseCase.execute(request);

    // Assert
    assertThat(response.otpSessionId()).isEqualTo(otpSessionId);
    assertThat(response.message())
        .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");

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
    String defaultPhone = TestDataProvider.defaultPhone;
    var request = new ForgotPasswordRequestDto(defaultPhone);

    when(phoneValidator.formatToE164(defaultPhone)).thenReturn(defaultPhone);
    when(userRepository.findByPhone(defaultPhone)).thenReturn(Optional.empty());

    // Act
    ForgotPasswordResponseDto response = forgotPasswordUseCase.execute(request);

    // Assert
    assertThat(response.otpSessionId().toString()).hasSize(36); // UUID Fake
    assertThat(response.message())
        .isEqualTo("Se o número estiver cadastrado, você receberá um código de verificação.");

    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o formato do telefone for inválido")
  void execute_shouldThrowException_whenPhoneFormatIsInvalid() {
    // Arrange
    String invalidPhone = "123456789";
    var requestDto = new ForgotPasswordRequestDto(invalidPhone);

    doThrow(new InvalidFormatPhoneException(ErrorCode.PHONE_INVALID_FORMAT))
        .when(phoneValidator)
        .formatToE164(invalidPhone);

    // Act & Assert
    assertThatThrownBy(() -> forgotPasswordUseCase.execute(requestDto))
        .isInstanceOf(InvalidFormatPhoneException.class)
        .satisfies(
            ex -> {
              InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_INVALID_FORMAT);
            });

    // Verify
    verify(phoneValidator, times(1)).formatToE164(invalidPhone);
    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }
}

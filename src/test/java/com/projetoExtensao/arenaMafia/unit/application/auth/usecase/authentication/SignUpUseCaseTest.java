package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.SignUpUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
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
@DisplayName("Testes unitários para SignUpUseCase")
public class SignUpUseCaseTest {

  @Mock private OtpSessionPort otpSessionPort;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private PasswordEncoderPort passwordEncoderPort;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private SignUpUseCaseImp signUpUseCase;

  @Test
  @DisplayName("Deve criar um novo usuário e publicar um evento quando os dados forem válidos")
  void execute_shouldCreateUserAndPublishEvent_whenDataIsValid() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();
    String encodedPassword = "encodedPassword";
    OtpSessionId otpSessionId = OtpSessionId.generate();
    var request =
        new SignupRequestDto(
            defaultUsername, defaultFullName, defaultPhone, defaultPassword, defaultPassword);

    when(phoneValidator.formatToE164(defaultPhone)).thenReturn(defaultPhone);
    when(userRepository.existsByUsername(defaultUsername)).thenReturn(false);
    when(userRepository.existsByPhone(defaultPhone)).thenReturn(false);
    when(passwordEncoderPort.encode(defaultPassword)).thenReturn(encodedPassword);
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(otpSessionPort.generateOtpSession(userId)).thenReturn(otpSessionId);

    // Act
    OtpSessionId response = signUpUseCase.execute(request);

    // Assert
    assertThat(response).isEqualTo(otpSessionId);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(1)).save(userCaptor.capture());
    User userBeingSaved = userCaptor.getValue();

    assertThat(userBeingSaved.getUsername()).isEqualTo(defaultUsername);
    assertThat(userBeingSaved.getFullName()).isEqualTo(defaultFullName);
    assertThat(userBeingSaved.getPhone()).isEqualTo(defaultPhone);
    assertThat(userBeingSaved.getPasswordHash()).isEqualTo(encodedPassword);
    assertThat(userBeingSaved.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
  }

  @Test
  @DisplayName("Deve lançar InvalidFormatPhoneException quando o telefone for inválido")
  void execute_shouldThrowInvalidFormatPhoneException_whenPhoneIsInvalid() {
    // Arrange
    String invalidPhone = "+999123456789";
    var request =
        new SignupRequestDto(
            defaultUsername, defaultFullName, invalidPhone, defaultPassword, defaultPassword);

    doThrow(new InvalidFormatPhoneException(ErrorCode.PHONE_INVALID_FORMAT))
        .when(phoneValidator)
        .formatToE164(invalidPhone);

    // Act & Assert
    assertThatThrownBy(() -> signUpUseCase.execute(request))
        .isInstanceOf(InvalidFormatPhoneException.class)
        .satisfies(
            ex -> {
              InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_INVALID_FORMAT);
            });

    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
  }

  @Test
  @DisplayName("Deve lançar UserAlreadyExistsException quando o telefone já existir")
  void execute_shouldThrowUserAlreadyExistsException_whenPhoneAlreadyExist() {
    // Arrange
    var request =
        new SignupRequestDto(
            defaultUsername, defaultFullName, defaultPhone, defaultPassword, defaultPassword);

    when(phoneValidator.formatToE164(defaultPhone)).thenReturn(defaultPhone);
    when(userRepository.existsByUsername(defaultUsername)).thenReturn(false);
    when(userRepository.existsByPhone(defaultPhone)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> signUpUseCase.execute(request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .satisfies(
            ex -> {
              UserAlreadyExistsException exception = (UserAlreadyExistsException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS);
            });

    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar UserAlreadyExistsException quando o username já existir")
  void execute_shouldThrowUserAlreadyExistsException_whenUsernameAlreadyExist() {
    // Arrange
    var request =
        new SignupRequestDto(
            defaultUsername, defaultFullName, defaultPhone, defaultPassword, defaultPassword);

    when(phoneValidator.formatToE164(defaultPhone)).thenReturn(defaultPhone);
    when(userRepository.existsByUsername(defaultUsername)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> signUpUseCase.execute(request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .satisfies(
            ex -> {
              UserAlreadyExistsException exception = (UserAlreadyExistsException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
            });

    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }
}

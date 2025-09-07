package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp.SignUpUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;
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
@DisplayName("Testes unitários para SignUpUseCase")
public class SignUpUseCaseTest {

  @Mock private OtpSessionPort otpSessionPort;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private PasswordEncoderPort passwordEncoderPort;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private SignUpUseCaseImp signUpUseCase;

  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPassword = "password123";
  private final String defaultConfirmPassword = "password123";
  private final String encodedPassword = "hashedPassword";
  private final String formattedPhone = "+558320548181";
  private final String unformattedPhone = "+558320548181";

  private User createUser() {
    return User.create(defaultUsername, defaultFullName, formattedPhone, encodedPassword);
  }

  @Test
  @DisplayName("Deve criar um novo usuário e publicar um evento quando os dados forem válidos")
  void execute_shouldCreateUserAndPublishEvent_whenDataIsValid() {
    // Arrange
    User savedUser = createUser();
    String otpSessionId = UUID.randomUUID().toString();
    var request =
        new SignupRequestDto(
            defaultUsername,
            defaultFullName,
            unformattedPhone,
            defaultPassword,
            defaultConfirmPassword);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.existsByUsername(defaultUsername)).thenReturn(false);
    when(userRepository.existsByPhone(formattedPhone)).thenReturn(false);
    when(passwordEncoderPort.encode(defaultPassword)).thenReturn(encodedPassword);
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(otpSessionPort.generateOtpSession(savedUser.getId())).thenReturn(otpSessionId);

    // Act
    String response = signUpUseCase.execute(request);

    // Assert
    assertThat(response).isEqualTo(otpSessionId);

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).existsByUsername(defaultUsername);
    verify(userRepository, times(1)).existsByPhone(formattedPhone);
    verify(userRepository, times(1)).save(any(User.class));
    verify(passwordEncoderPort, times(1)).encode(defaultPassword);
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
    verify(otpSessionPort, times(1)).generateOtpSession(savedUser.getId());
  }

  @Nested
  @DisplayName("Deve lançar exceção quando o usuário já existir")
  class UserAlreadyExistsTests {
    @Test
    @DisplayName("Deve lançar UserAlreadyExistsException se o telefone já existir ")
    void execute_shouldThrowException_whenPhoneAlreadyExists() {
      // Arrange
      var request =
          new SignupRequestDto(
              defaultUsername,
              defaultFullName,
              unformattedPhone,
              defaultPassword,
              defaultConfirmPassword);

      when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
      when(userRepository.existsByUsername(defaultUsername)).thenReturn(false);
      when(userRepository.existsByPhone(formattedPhone)).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> signUpUseCase.execute(request))
          .isInstanceOf(UserAlreadyExistsException.class)
          .hasMessage("Esse número de telefone já está em uso.");

      verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
      verify(userRepository, times(1)).existsByUsername(defaultUsername);
      verify(userRepository, times(1)).existsByPhone(formattedPhone);
      verify(passwordEncoderPort, never()).encode(defaultPassword);
      verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
      verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    }

    @Test
    @DisplayName("Deve lançar UserAlreadyExistsException se username já existir ")
    void execute_shouldThrowExceptionWhenUsernameAlreadyExists() {
      // Arrange
      var request =
          new SignupRequestDto(
              defaultUsername,
              defaultFullName,
              unformattedPhone,
              defaultPassword,
              defaultConfirmPassword);

      when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
      when(userRepository.existsByUsername(defaultUsername)).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> signUpUseCase.execute(request))
          .isInstanceOf(UserAlreadyExistsException.class)
          .hasMessage("Esse nome de usuário já está em uso.");

      verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
      verify(userRepository, times(1)).existsByUsername(defaultUsername);
      verify(userRepository, never()).existsByPhone(formattedPhone);
      verify(passwordEncoderPort, never()).encode(defaultPassword);
      verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
      verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
    }
  }

  @Test
  @DisplayName("Deve lançar BadPhoneNumberException quando o telefone for inválido")
  void execute_shouldThrowBadPhoneNumberException_whenPhoneIsInvalid() {
    // Arrange
    String errorMessage = "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.";
    String invalidPhone = "+123456789";
    var request =
        new SignupRequestDto(
            defaultUsername,
            defaultFullName,
            invalidPhone,
            defaultPassword,
            defaultConfirmPassword);

    doThrow(new BadPhoneNumberException(errorMessage))
        .when(phoneValidator)
        .formatToE164(invalidPhone);

    // Act & Assert
    assertThatThrownBy(() -> signUpUseCase.execute(request))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage(errorMessage);

    verify(phoneValidator, times(1)).formatToE164(invalidPhone);
    verify(userRepository, never()).existsByUsername(defaultUsername);
    verify(userRepository, never()).existsByPhone(formattedPhone);
    verify(passwordEncoderPort, never()).encode(defaultPassword);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
    verify(otpSessionPort, never()).generateOtpSession(any(UUID.class));
  }
}

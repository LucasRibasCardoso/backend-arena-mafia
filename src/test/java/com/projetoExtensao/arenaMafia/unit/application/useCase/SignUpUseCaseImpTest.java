package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.SignUpUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("Testes unitários para SignUpUseCase")
public class SignUpUseCaseImpTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private PasswordEncoderPort passwordEncoderPort;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private SignUpUseCaseImp signUpUseCase;

  private SignupRequestDto requestDto;

  @BeforeEach
  void setUp() {
    requestDto =
        new SignupRequestDto(
            "testuser", "Test User Full Name", "+5547999998888", "password123", "password123");
  }

  @Nested
  @DisplayName("Cenários de Sucesso")
  class SuccessScenarios {

    @Test
    @DisplayName("Deve criar um novo usuário e publicar um evento quando os dados são válidos")
    void execute_shouldCreateUserAndPublishEvent_whenDataIsValid() {
      // Arrange
      String formattedPhone = "+5547999998888";
      String encodedPassword = "hashedPassword";

      when(userRepository.existsByUsername(anyString())).thenReturn(false);
      when(userRepository.existsByPhone(anyString())).thenReturn(false);

      when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(formattedPhone);
      when(passwordEncoderPort.encode(requestDto.password())).thenReturn(encodedPassword);

      User userToSave =
          User.create(
              requestDto.username(), requestDto.fullName(), formattedPhone, encodedPassword);
      when(userRepository.save(any(User.class))).thenReturn(userToSave);

      // Act
      String result = signUpUseCase.execute(requestDto);

      // Assert
      ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userArgumentCaptor.capture());
      User savedUser = userArgumentCaptor.getValue();

      assertThat(savedUser.getUsername()).isEqualTo(requestDto.username());
      assertThat(savedUser.getFullName()).isEqualTo(requestDto.fullName());
      assertThat(savedUser.getPhone()).isEqualTo(formattedPhone);
      assertThat(savedUser.getPasswordHash()).isEqualTo(encodedPassword);

      ArgumentCaptor<OnVerificationRequiredEvent> eventArgumentCaptor =
          ArgumentCaptor.forClass(OnVerificationRequiredEvent.class);
      verify(eventPublisher).publishEvent(eventArgumentCaptor.capture());

      assertThat(requestDto.phone()).isEqualTo(result);
    }
  }

  @Nested
  @DisplayName("Cenários de Falha")
  class FailureScenarios {

    @Test
    @DisplayName("Deve lançar UserAlreadyExistsException se o telefone já existir ")
    void execute_shouldThrowExceptionWhenPhoneAlreadyExists() {
      // Arrange
      when(userRepository.existsByPhone(requestDto.phone())).thenReturn(true);
      when(userRepository.existsByUsername(requestDto.username())).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> signUpUseCase.execute(requestDto))
          .isInstanceOf(UserAlreadyExistsException.class)
          .hasMessage("Esse número de telefone já está em uso.");

      verify(userRepository, never()).save(any(User.class));
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Deve lançar UserAlreadyExistsException se username já existir ")
    void execute_shouldThrowExceptionWhenUsernameAlreadyExists() {
      // Arrange
      when(userRepository.existsByUsername(requestDto.username())).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> signUpUseCase.execute(requestDto))
          .isInstanceOf(UserAlreadyExistsException.class)
          .hasMessage("Esse nome de usuário já está em uso.");

      verify(eventPublisher, never()).publishEvent(any());
    }
  }
}

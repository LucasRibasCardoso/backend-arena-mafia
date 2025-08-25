package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.event.UserRegisteredEvent;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.SignUpUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.user.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.SignupRequestDto;
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
import org.springframework.dao.DataIntegrityViolationException;

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

      // Simula que o usuário não existe
      when(userRepository.existsByUsernameOrPhone(requestDto.username(), requestDto.phone()))
          .thenReturn(false);

      // Simula o comportamento dos helpers
      when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(formattedPhone);
      when(passwordEncoderPort.encode(requestDto.password())).thenReturn(encodedPassword);

      // Simula a criação do usuário para capturar o objeto
      User userToSave =
          User.create(
              requestDto.username(), requestDto.fullName(), formattedPhone, encodedPassword);
      when(userRepository.save(any(User.class))).thenReturn(userToSave);

      // Act
      String resultUsername = signUpUseCase.execute(requestDto);

      // Assert
      ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userArgumentCaptor.capture());
      User savedUser = userArgumentCaptor.getValue();

      // 2. Verifica se os dados do usuário salvo estão corretos
      assertThat(savedUser.getUsername()).isEqualTo(requestDto.username());
      assertThat(savedUser.getFullName()).isEqualTo(requestDto.fullName());
      assertThat(savedUser.getPhone()).isEqualTo(formattedPhone);
      assertThat(savedUser.getPasswordHash()).isEqualTo(encodedPassword);

      // 3. Verifica se o evento foi publicado corretamente
      ArgumentCaptor<UserRegisteredEvent> eventArgumentCaptor =
          ArgumentCaptor.forClass(UserRegisteredEvent.class);
      verify(eventPublisher).publishEvent(eventArgumentCaptor.capture());

      assertThat(requestDto.username()).isEqualTo(resultUsername);
    }
  }

  @Nested
  @DisplayName("Cenários de Falha")
  class FailureScenarios {

    @Test
    @DisplayName(
        "Deve lançar UserAlreadyExistsException se o usuário ou telefone já existirem (pré-verificação)")
    void execute_shouldThrowException_whenUserOrPhoneAlreadyExists() {
      // Arrange
      when(userRepository.existsByUsernameOrPhone(requestDto.username(), requestDto.phone()))
          .thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> signUpUseCase.execute(requestDto))
          .isInstanceOf(UserAlreadyExistsException.class)
          .hasMessage("Nome de usuário ou telefone indisponível. Por favor, utilize outros.");

      verify(userRepository, never()).save(any(User.class));
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName(
        "Deve lançar UserAlreadyExistsException se ocorrer DataIntegrityViolationException ao salvar")
    void execute_shouldThrowException_whenDataIntegrityViolationOccurs() {
      // Arrange
      // Simula que o usuário não existe na pré-verificação...
      when(userRepository.existsByUsernameOrPhone(requestDto.username(), requestDto.phone()))
          .thenReturn(false);

      // Lança uma exceção de integridade ao tentar salvar (simulando uma race condition)
      when(userRepository.save(any(User.class)))
          .thenThrow(new DataIntegrityViolationException("Erro de duplicidade no banco"));

      // Mocks dos helpers
      when(phoneValidator.formatToE164(anyString())).thenReturn("+5547999998888");
      when(passwordEncoderPort.encode(anyString())).thenReturn("hashedPassword");

      // Act & Assert
      assertThatThrownBy(() -> signUpUseCase.execute(requestDto))
          .isInstanceOf(UserAlreadyExistsException.class)
          .hasMessage("Nome de usuário ou telefone indisponível. Por favor, utilize outros.");

      verify(eventPublisher, never()).publishEvent(any());
    }
  }
}

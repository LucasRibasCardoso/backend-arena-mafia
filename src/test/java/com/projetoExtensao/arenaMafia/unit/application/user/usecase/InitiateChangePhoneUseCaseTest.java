package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp.InitiateChangePhoneUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.InitiateChangePhoneRequestDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para InitiateChangePhoneUseCase")
public class InitiateChangePhoneUseCaseTest {

  @Mock private PendingPhoneChangePort pendingPhoneChangePort;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private PhoneValidatorPort phoneValidatorPort;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private InitiateChangePhoneUseCaseImp initiateChangePhoneUseCase;

  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPhone = "+558320548181";
  private final String defaultPassword = "123456";

  @Test
  @DisplayName("Deve iniciar o processo de mudança de telefone")
  void execute_shouldInitiatePhoneChangeProcess() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();

    String newPhone = "+558320566921";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone)).thenReturn(newPhone);
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));
    when(userRepository.findByPhone(newPhone)).thenReturn(Optional.empty());

    // Act
    initiateChangePhoneUseCase.execute(idCurrentUser, request);

    // Assert
    verify(phoneValidatorPort, times(1)).formatToE164(newPhone);
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(1)).findByPhone(newPhone);
    verify(pendingPhoneChangePort, times(1)).save(idCurrentUser, newPhone);
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o telefone novo for inválido")
  void execute_shouldThrowException_whenNewPhoneIsInvalid() {
    // Arrange
    String errorMessage = "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.";
    UUID idCurrentUser = UUID.randomUUID();
    String newPhone = "+999999999999";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone))
        .thenThrow(new BadPhoneNumberException(errorMessage));

    // Act & Assert
    assertThatThrownBy(() -> initiateChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage(errorMessage);

    verify(phoneValidatorPort, times(1)).formatToE164(newPhone);
    verify(userRepository, never()).findById(idCurrentUser);
    verify(userRepository, never()).findByPhone(newPhone);
    verify(pendingPhoneChangePort, never()).save(idCurrentUser, newPhone);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o telefone novo já estiver em uso")
  void execute_shouldThrowException_whenNewPhoneIsAlreadyInUse() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = UUID.randomUUID();
    String newPhone = "+558320566921";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone)).thenReturn(newPhone);
    when(userRepository.findByPhone(newPhone)).thenReturn(Optional.of(mockUser));

    // Act & Assert
    assertThatThrownBy(() -> initiateChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessage("Esse número de telefone já está em uso.");

    verify(phoneValidatorPort, times(1)).formatToE164(newPhone);
    verify(userRepository, never()).findById(any(UUID.class));
    verify(userRepository, times(1)).findByPhone(newPhone);
    verify(pendingPhoneChangePort, never()).save(idCurrentUser, newPhone);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserIsNotFound() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    String newPhone = "+558320566921";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone)).thenReturn(newPhone);
    when(userRepository.findByPhone(newPhone)).thenReturn(Optional.of(mockUser));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> initiateChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("Usuário não encontrado.");

    verify(phoneValidatorPort, times(1)).formatToE164(newPhone);
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(1)).findByPhone(newPhone);
    verify(pendingPhoneChangePort, never()).save(idCurrentUser, newPhone);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }
}

package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp.InitiateChangePhoneUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.InitiateChangePhoneRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
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

  @Test
  @DisplayName("Deve iniciar o processo de mudança de telefone")
  void execute_shouldInitiatePhoneChangeProcess() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();

    String newPhone = "+558320566921";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone)).thenReturn(newPhone);
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));
    when(userRepository.findByPhone(newPhone)).thenReturn(Optional.empty());

    // Act
    initiateChangePhoneUseCase.execute(idCurrentUser, request);

    // Assert
    verify(pendingPhoneChangePort, times(1)).save(idCurrentUser, newPhone);
    verify(eventPublisher, times(1)).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidFormatPhoneException quando o telefone novo for inválido")
  void execute_shouldThrowInvalidFormatPhoneException_whenNewPhoneIsInvalid() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    String newPhone = "+999999999999";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone))
        .thenThrow(new InvalidFormatPhoneException(ErrorCode.PHONE_INVALID_FORMAT));

    // Act & Assert
    assertThatThrownBy(() -> initiateChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidFormatPhoneException.class)
        .satisfies(
            ex -> {
              InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_INVALID_FORMAT);
            });

    verify(pendingPhoneChangePort, never()).save(idCurrentUser, newPhone);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar UserAlreadyExistsException quando o telefone novo já estiver em uso")
  void execute_shouldThrowException_whenNewPhoneIsAlreadyInUse() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = UUID.randomUUID();
    String newPhone = "+558320566921";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone)).thenReturn(newPhone);
    when(userRepository.findByPhone(newPhone)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> initiateChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .satisfies(
            ex -> {
              UserAlreadyExistsException exception = (UserAlreadyExistsException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS);
            });

    verify(pendingPhoneChangePort, never()).save(idCurrentUser, newPhone);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserIsNotFound() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    String newPhone = "+558320566921";
    var request = new InitiateChangePhoneRequestDto(newPhone);

    when(phoneValidatorPort.formatToE164(newPhone)).thenReturn(newPhone);
    when(userRepository.findByPhone(newPhone)).thenReturn(Optional.of(user));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> initiateChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(pendingPhoneChangePort, never()).save(idCurrentUser, newPhone);
    verify(eventPublisher, never()).publishEvent(any(OnVerificationRequiredEvent.class));
  }
}

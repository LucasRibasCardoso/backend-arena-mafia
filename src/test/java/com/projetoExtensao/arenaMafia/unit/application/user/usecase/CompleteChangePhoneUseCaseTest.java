package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp.CompleteChangePhoneUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPhoneChangeRequestException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPhoneException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.CompletePhoneChangeRequestDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para CompleteChangePhoneUseCase")
public class CompleteChangePhoneUseCaseTest {

  @Mock private OtpPort otpPort;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PendingPhoneChangePort pendingPhoneChangePort;
  @InjectMocks private CompleteChangePhoneUseCaseImp completeChangePhoneUseCase;

  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPhone = "+558320548181";
  private final String defaultPassword = "123456";

  private final OtpCode otpCode = OtpCode.generate();

  @Test
  @DisplayName("Deve completar o processo de mudança de telefone")
  void execute_shouldCompletePhoneChangeProcess() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    String newPhone = "+558320566921";
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.of(newPhone));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));
    when(userRepository.save(mockUser)).thenReturn(mockUser);

    // Act
    User updatedUser = completeChangePhoneUseCase.execute(idCurrentUser, request);

    // Assert
    assertThat(updatedUser.getPhone()).isEqualTo(newPhone);

    verify(otpPort, times(1)).validateOtp(idCurrentUser, otpCode);
    verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(idCurrentUser);
    verify(pendingPhoneChangePort, times(1)).deleteByUserId(idCurrentUser);
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(1)).save(mockUser);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "telefone-invalido", "123456"})
  @DisplayName("Deve lançar exceção (de User.validatePhone) quando o telefone salvo for inválido")
  void execute_shouldThrowInvalidPhoneException_whenSavedPhoneIsInvalid(String invalidPhoneFormat) {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser))
        .thenReturn(Optional.of(invalidPhoneFormat));

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidPhoneException.class);

    verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(idCurrentUser);
    verify(otpPort, never()).validateOtp(any(), any(OtpCode.class));
    verify(pendingPhoneChangePort, never()).deleteByUserId(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar exceção quando a solicitação de mudança de telefone estiver expirada")
  void execute_shouldThrowException_whenPhoneChangeRequestExpired() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidPhoneChangeRequestException.class)
        .hasMessage("Sua solicitação de alteração de telefone já expirou. Tente novamente.");

    verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(idCurrentUser);
    verify(otpPort, never()).validateOtp(any(), any(OtpCode.class));
    verify(pendingPhoneChangePort, never()).deleteByUserId(any());
    verify(userRepository, never()).findById(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar exceção quando o código OTP for inválido")
  void execute_shouldThrowException_whenOtpCodeIsInvalid() {
    // Arrange
    String errorMessage = "Código de verificação inválido ou expirado.";
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    String newPhone = "+558320566921";
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.of(newPhone));
    doThrow(new InvalidOtpException(errorMessage))
        .when(otpPort)
        .validateOtp(idCurrentUser, otpCode);

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage(errorMessage);

    verify(otpPort, times(1)).validateOtp(idCurrentUser, otpCode);
    verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(idCurrentUser);
    verify(pendingPhoneChangePort, never()).deleteByUserId(idCurrentUser);
    verify(userRepository, never()).findById(idCurrentUser);
    verify(userRepository, never()).save(mockUser);
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserIsNotFound() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    String newPhone = "+558320566921";
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.of(newPhone));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(
            com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException.class)
        .hasMessage("Usuário não encontrado.");

    verify(otpPort, times(1)).validateOtp(idCurrentUser, otpCode);
    verify(pendingPhoneChangePort, times(1)).findPhoneByUserId(idCurrentUser);
    verify(pendingPhoneChangePort, times(1)).deleteByUserId(idCurrentUser);
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, never()).save(mockUser);
  }
}

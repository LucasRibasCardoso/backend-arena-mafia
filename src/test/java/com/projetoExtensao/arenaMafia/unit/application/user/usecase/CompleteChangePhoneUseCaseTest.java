package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.phone.imp.CompleteChangePhoneUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PhoneChangeNotInitiatedException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.CompletePhoneChangeRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

  private final OtpCode otpCode = OtpCode.generate();

  @Test
  @DisplayName("Deve completar o processo de mudança de telefone")
  void execute_shouldCompletePhoneChangeProcess() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    String newPhone = "+558320566921";
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.of(newPhone));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    // Act
    User updatedUser = completeChangePhoneUseCase.execute(idCurrentUser, request);

    // Assert
    assertThat(updatedUser.getPhone()).isEqualTo(newPhone);
    verify(userRepository, times(1)).save(user);
  }

  @ParameterizedTest
  @MethodSource("com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidPhoneProvider")
  @DisplayName("Deve lançar InvalidFormatPhoneException quando o telefone novo for inválido")
  void execute_shouldThrowInvalidFormatPhoneException_whenNewPhoneIsInvalid(
      String phone, ErrorCode errorCode) {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = UUID.randomUUID();
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser))
        .thenReturn(Optional.ofNullable(phone));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidFormatPhoneException.class)
        .satisfies(
            ex -> {
              InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            });

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "Deve lançar PhoneChangeNotInitiatedException quando a solicitação de mudança de telefone estiver expirada")
  void execute_shouldThrowPhoneChangeNotInitiatedException_whenPhoneChangeRequestExpired() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(PhoneChangeNotInitiatedException.class)
        .satisfies(
            ex -> {
              PhoneChangeNotInitiatedException exception = (PhoneChangeNotInitiatedException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_CHANGE_NOT_INITIATED);
            });

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException quando o código OTP for inválido")
  void execute_shouldThrowException_whenOtpCodeIsInvalid() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    String newPhone = "+558320566921";
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.of(newPhone));
    doThrow(new InvalidOtpException(ErrorCode.OTP_CODE_INVALID_OR_EXPIRED))
        .when(otpPort)
        .validateOtp(idCurrentUser, otpCode);

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidOtpException.class)
        .satisfies(
            ex -> {
              InvalidOtpException exception = (InvalidOtpException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OTP_CODE_INVALID_OR_EXPIRED);
            });

    verify(userRepository, never()).save(user);
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserIsNotFound() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    String newPhone = "+558320566921";
    var request = new CompletePhoneChangeRequestDto(otpCode);

    when(pendingPhoneChangePort.findPhoneByUserId(idCurrentUser)).thenReturn(Optional.of(newPhone));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> completeChangePhoneUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(userRepository, never()).save(user);
  }
}

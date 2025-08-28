package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.ForgotPassword.imp.GeneratePasswordResetTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tetes unitários para ValidateResetTokenUseCase")
public class GeneratePasswordResetTokenUseCaseImpTest {

  @Mock private OtpPort otpPort;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PasswordResetTokenPort passwordResetToken;

  @InjectMocks private GeneratePasswordResetTokenUseCaseImp generatePasswordResetTokenUseCase;

  @Test
  @DisplayName("Deve gerar e retornar um token de redefinição para uma requisição válida")
  void execute_shouldGenerateAndReturnResetToken_forValidRequest() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto("+5547988887777", "123456");
    String formattedPhone = "+5547988887777";
    User mockUser = mock(User.class);
    UUID userId = UUID.randomUUID();
    String expectedToken = UUID.randomUUID().toString();

    when(mockUser.getId()).thenReturn(userId);
    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(mockUser));
    doNothing().when(otpPort).validateOtp(userId, requestDto.code());
    when(passwordResetToken.save(userId)).thenReturn(expectedToken);

    // Act
    String actualToken = generatePasswordResetTokenUseCase.execute(requestDto);

    // Assert
    assertThat(actualToken).isEqualTo(expectedToken);
    verify(phoneValidator, times(1)).formatToE164(requestDto.phone());
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, times(1)).validateOtp(userId, requestDto.code());
    verify(passwordResetToken, times(1)).save(userId);
  }

  @Test
  @DisplayName("Deve propagar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldPropagateUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto("+5547988887777", "123456");
    String formattedPhone = "+5547988887777";

    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(requestDto))
        .isInstanceOf(UserNotFoundException.class);

    // Verify
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(passwordResetToken, never()).save(any());
  }

  @Test
  @DisplayName("Deve propagar InvalidOtpException quando o código OTP for inválido")
  void execute_shouldPropagateInvalidOtpException_whenOtpIsInvalid() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto("+5547988887777", "wrong-code");
    String formattedPhone = "+5547988887777";
    User mockUser = mock(User.class);
    UUID userId = UUID.randomUUID();

    when(mockUser.getId()).thenReturn(userId);
    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(mockUser));
    doThrow(new InvalidOtpException("Código inválido."))
        .when(otpPort)
        .validateOtp(userId, "wrong-code");

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(requestDto))
        .isInstanceOf(InvalidOtpException.class);

    verify(passwordResetToken, never()).save(any());
  }
}

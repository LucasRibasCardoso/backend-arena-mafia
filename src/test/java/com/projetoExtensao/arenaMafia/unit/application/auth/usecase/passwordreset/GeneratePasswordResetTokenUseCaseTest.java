package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.GeneratePasswordResetTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para GeneratePasswordResetToken")
public class GeneratePasswordResetTokenUseCaseTest {

  @Mock private OtpPort otpPort;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PasswordResetTokenPort passwordResetToken;

  @InjectMocks private GeneratePasswordResetTokenUseCaseImp generatePasswordResetTokenUseCase;

  private final String defaultPhone = "+558320548181";
  private final String unformattedPhone = "+558320548181";
  private final String formattedPhone = defaultPhone;
  private final String defaultOtp = "123456";

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        defaultPhone,
        "123456",
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve gerar e retornar um token de redefinição para uma requisição válida")
  void execute_shouldGenerateAndReturnResetToken_forValidRequest() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    String expectedToken = "reset-token";
    var request = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));
    when(passwordResetToken.save(user.getId())).thenReturn(expectedToken);

    // Act
    String response = generatePasswordResetTokenUseCase.execute(request);

    // Assert
    assertThat(response).isEqualTo(expectedToken);
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, times(1)).validateOtp(user.getId(), defaultOtp);
    verify(passwordResetToken, times(1)).save(user.getId());
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando o número de telefone for inválido")
  void execute_shouldThrowException_whenPhoneNumberIsInvalid() {
    // Arrange
    String errorMessage = "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.";
    String invalidPhone = "123456789";
    var request = new ValidateOtpRequestDto(invalidPhone, defaultOtp);

    when(phoneValidator.formatToE164(invalidPhone))
        .thenThrow(new BadPhoneNumberException(errorMessage));

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage(errorMessage);

    verify(phoneValidator, times(1)).formatToE164(invalidPhone);
    verify(userRepository, never()).findByPhone(anyString());
    verify(otpPort, never()).validateOtp(any(), any());
    verify(passwordResetToken, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(requestDto))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage(
            "Usuário não encontrado. Verifique o número de telefone informado e tente novamente.");

    // Verify
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, never()).validateOtp(any(), any());
    verify(passwordResetToken, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException quando o código OTP for inválido")
  void execute_shouldThrowInvalidOtpException_whenOtpIsInvalid() {
    // Arrange
    String errorMessage = "Código de verificação inválido ou expirado.";
    User user = createUser(AccountStatus.ACTIVE);
    String invalidOtp = "000000";
    var requestDto = new ValidateOtpRequestDto(unformattedPhone, invalidOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));
    doThrow(new InvalidOtpException(errorMessage))
        .when(otpPort)
        .validateOtp(user.getId(), invalidOtp);

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(requestDto))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage(errorMessage);

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, times(1)).validateOtp(user.getId(), invalidOtp);
    verify(passwordResetToken, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar AccountStateConflictException quando a conta estiver bloqueada")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsLocked() {
    // Arrange
    User user = createUser(AccountStatus.LOCKED);
    var request = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, never()).validateOtp(any(), any());
    verify(passwordResetToken, never()).save(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStateConflictException quando a conta estiver pendente de verificação")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsPendingVerification() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    var request = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, never()).validateOtp(any(), any());
    verify(passwordResetToken, never()).save(any());
  }

  @Test
  @DisplayName("Deve lançar AccountStateConflictException quando a conta estiver desativada")
  void execute_shouldThrowAccountStateConflictException_whenAccountIsDisabled() {
    // Arrange
    User user = createUser(AccountStatus.DISABLED);
    var request = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> generatePasswordResetTokenUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Sua conta está desativada e será deletada em breve. Para reativá-la, por favor, entre em contato com o suporte.");

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, never()).validateOtp(any(), any());
    verify(passwordResetToken, never()).save(any());
  }
}

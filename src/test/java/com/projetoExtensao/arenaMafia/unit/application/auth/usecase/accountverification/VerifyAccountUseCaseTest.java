package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.accountverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp.VerifyAccountUseCaseImp;
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
@DisplayName("Testes unitários para VerifyAccountUseCase")
public class VerifyAccountUseCaseTest {

  @Mock private AuthPort authPort;
  @Mock private OtpPort otpPort;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private VerifyAccountUseCaseImp verifyAccountUseCase;

  private final String defaultPhone = "+558320548181";
  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPassword = "123456";

  private final String unformattedPhone = "+558320548181";
  private final String formattedPhone = defaultPhone;
  private final String defaultOtp = "123456";

  private User createUser() {
    return User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
  }

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        defaultUsername,
        defaultFullName,
        defaultPhone,
        defaultPassword,
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve verificar e ativar a conta do usuário e retornar informações de autenticação")
  void execute_shouldActivateAccountAndReturnTokens_whenUserIsValid() {
    // Arrange
    User user = createUser();
    String accessToken = "access_token";
    String refreshToken = "refresh_token";
    var request = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);
    var expectedResponse = new AuthResult(user, accessToken, refreshToken);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));
    doNothing().when(otpPort).validateOtp(user.getId(), defaultOtp);
    when(authPort.generateTokens(user)).thenReturn(expectedResponse);

    // Act
    AuthResult result = verifyAccountUseCase.execute(request);

    // Assert
    assertThat(result.accessToken()).isEqualTo(expectedResponse.accessToken());
    assertThat(result.refreshToken()).isEqualTo(expectedResponse.refreshToken());
    assertThat(result.user().getPhone()).isEqualTo(user.getPhone());
    assertThat(result.user().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.user().getFullName()).isEqualTo(user.getFullName());
    assertThat(result.user().getRole()).isEqualTo(user.getRole());

    // Verify
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, times(1)).validateOtp(user.getId(), defaultOtp);
    verify(userRepository, times(1)).save(user);
    verify(authPort, times(1)).generateTokens(user);
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando o número de telefone for inválido")
  void execute_shouldThrowException_whenPhoneNumberIsInvalid() {
    // Arrange
    String errorMessage = "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.";
    String invalidPhone = "123456789";
    String code = "123456";
    var requestDto = new ValidateOtpRequestDto(invalidPhone, code);

    when(phoneValidator.formatToE164(invalidPhone))
        .thenThrow(new BadPhoneNumberException(errorMessage));

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage(errorMessage);

    verify(phoneValidator, times(1)).formatToE164(invalidPhone);
    verify(userRepository, never()).findByPhone(anyString());
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    var request = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage(
            "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta.");

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException para um código de verificação inválido")
  void execute_shouldThrowInvalidOtpException_forInvalidOtpCode() {
    // Arrange
    String errorMessage = "Código de verificação inválido ou expirado.";
    User user = createUser();
    String invalidCode = "999999";
    var requestDto = new ValidateOtpRequestDto(unformattedPhone, invalidCode);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(user));
    doThrow(new InvalidOtpException(errorMessage))
        .when(otpPort)
        .validateOtp(user.getId(), invalidCode);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage(errorMessage);

    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, times(1)).validateOtp(user.getId(), invalidCode);
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção ao tentar verificar uma conta que já está ativa")
  void execute_shouldThrowException_whenAccountIsNotPendingVerification() {
    // Arrange
    User activeUser = createUser(AccountStatus.ACTIVE);
    var requestDto = new ValidateOtpRequestDto(unformattedPhone, defaultOtp);

    when(phoneValidator.formatToE164(unformattedPhone)).thenReturn(formattedPhone);
    when(userRepository.findByPhone(formattedPhone)).thenReturn(Optional.of(activeUser));
    doNothing().when(otpPort).validateOtp(activeUser.getId(), defaultOtp);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Não é possível ativar uma conta que não está pendente de verificação.");

    // Verify
    verify(phoneValidator, times(1)).formatToE164(unformattedPhone);
    verify(userRepository, times(1)).findByPhone(formattedPhone);
    verify(otpPort, times(1)).validateOtp(activeUser.getId(), defaultOtp);
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }
}

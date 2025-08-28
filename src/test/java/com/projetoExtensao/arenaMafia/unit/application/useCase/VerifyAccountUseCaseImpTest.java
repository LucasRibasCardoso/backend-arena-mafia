package com.projetoExtensao.arenaMafia.unit.application.useCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.VerifyAccountUseCaseImp;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para VerifyAccountUseCase")
public class VerifyAccountUseCaseImpTest {

  @Mock private AuthPort authPort;
  @Mock private OtpPort otpPort;
  @Mock private PhoneValidatorPort phoneValidator;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private VerifyAccountUseCaseImp verifyAccountUseCase;

  @Test
  @DisplayName("Deve ativar a conta e retornar tokens para um usuário e OTP válidos")
  void execute_shouldActivateAccountAndReturnTokens_forValidUserAndOtp() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto("+5547912345678", "123456");
    User userPendingVerification =
        User.create("testuser", "Test User", "+5547912345678", "hashedPassword");

    var expectedAuthResult = new AuthResult("testuser", "access_token", "refresh_token");

    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(requestDto.phone());
    when(userRepository.findByPhone(requestDto.phone()))
        .thenReturn(Optional.of(userPendingVerification));
    doNothing().when(otpPort).validateOtp(userPendingVerification.getId(), "123456");
    when(authPort.generateTokens(any(User.class))).thenReturn(expectedAuthResult);

    // Act
    AuthResult actualAuthResult = verifyAccountUseCase.execute(requestDto);

    // Assert
    assertThat(actualAuthResult).isNotNull();
    assertThat(actualAuthResult.accessToken()).isEqualTo("access_token");

    // Verify
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    assertThat(savedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);

    verify(userRepository, times(1)).findByPhone(requestDto.phone());
    verify(otpPort, times(1)).validateOtp(userPendingVerification.getId(), requestDto.code());
    verify(authPort, times(1)).generateTokens(savedUser);
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto("+554799999999", "123456");
    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(requestDto.phone());
    when(userRepository.findByPhone(requestDto.phone())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage(
            "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta.");

    // Verify
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(userRepository, times(1)).findByPhone(requestDto.phone());
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException para um código de verificação inválido")
  void execute_shouldThrowInvalidOtpException_forInvalidOtpCode() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto("+5547912345678", "999999");
    User userPendingVerification =
        User.create("testuser", "Test User", "+5547912345678", "hashedPassword");

    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(requestDto.phone());
    when(userRepository.findByPhone(requestDto.phone()))
        .thenReturn(Optional.of(userPendingVerification));
    doThrow(new InvalidOtpException("Código de verificação inválido ou expirado."))
        .when(otpPort)
        .validateOtp(userPendingVerification.getId(), requestDto.code());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage("Código de verificação inválido ou expirado.");

    // Verify
    verify(userRepository, times(1)).findByPhone(requestDto.phone());
    verify(otpPort, times(1)).validateOtp(userPendingVerification.getId(), requestDto.code());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção ao tentar verificar uma conta que já está ativa")
  void execute_shouldThrowException_whenAccountIsNotPendingVerification() {
    // Arrange
    var requestDto = new ValidateOtpRequestDto("+5547912345678", "123456");

    User activeUser =
        User.reconstitute(
            UUID.randomUUID(),
            "testuser",
            "Test User",
            "+5547912345678",
            "hashedPassword",
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            Instant.now());

    when(phoneValidator.formatToE164(requestDto.phone())).thenReturn(requestDto.phone());
    when(userRepository.findByPhone(requestDto.phone())).thenReturn(Optional.of(activeUser));
    doNothing().when(otpPort).validateOtp(activeUser.getId(), requestDto.code());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: A conta já está ativada.");

    // Verify
    verify(userRepository, times(1)).findByPhone(requestDto.phone());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }
}

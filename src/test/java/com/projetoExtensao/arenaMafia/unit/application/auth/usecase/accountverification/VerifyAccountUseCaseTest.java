package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.accountverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp.VerifyAccountUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUserIdentifierException;
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
  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private VerifyAccountUseCaseImp verifyAccountUseCase;

  private final String defaultOtp = "123456";

  private User createUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "123456",
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Test
  @DisplayName("Deve verificar e ativar a conta do usuário e retornar informações de autenticação")
  void execute_shouldActivateAccountAndReturnTokens_whenUserIsValid() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    String accessToken = "access_token";
    String refreshToken = "refresh_token";
    UUID userId = user.getId();
    var request = new ValidateOtpRequestDto(userId.toString(), defaultOtp);
    var expectedResponse = new AuthResult(user, accessToken, refreshToken);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(authPort.generateTokens(user)).thenReturn(expectedResponse);

    // Act
    AuthResult result = verifyAccountUseCase.execute(request);

    // Assert
    assertThat(result.accessToken()).isEqualTo(expectedResponse.accessToken());
    assertThat(result.refreshToken()).isEqualTo(expectedResponse.refreshToken());
    assertThat(result.user().getId()).isEqualTo(user.getId());
    assertThat(result.user().getPhone()).isEqualTo(user.getPhone());
    assertThat(result.user().getUsername()).isEqualTo(user.getUsername());
    assertThat(result.user().getFullName()).isEqualTo(user.getFullName());
    assertThat(result.user().getRole()).isEqualTo(user.getRole());

    // Verify
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(user.getId(), defaultOtp);
    verify(userRepository, times(1)).save(user);
    verify(authPort, times(1)).generateTokens(user);
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando o userId for inválido")
  void execute_shouldThrowException_whenUserIdIsInvalid() {
    // Arrange
    String userId = "invalid-uuid";
    String code = "123456";
    var requestDto = new ValidateOtpRequestDto(userId, code);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(InvalidUserIdentifierException.class)
        .hasMessage("Identificador de usuário inválido.");

    verify(userRepository, never()).findById(any(UUID.class));
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    var request = new ValidateOtpRequestDto(userId.toString(), defaultOtp);

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage(
            "Usuário não encontrado. Retorne ao início do cadastro para criar uma nova conta.");

    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, never()).validateOtp(any(), anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException para um código de verificação inválido")
  void execute_shouldThrowInvalidOtpException_forInvalidOtpCode() {
    // Arrange
    String errorMessage = "Código de verificação inválido ou expirado.";
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    UUID userId = user.getId();
    String invalidCode = "aaabbb";
    var requestDto = new ValidateOtpRequestDto(userId.toString(), invalidCode);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    doThrow(new InvalidOtpException(errorMessage))
        .when(otpPort)
        .validateOtp(user.getId(), invalidCode);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage(errorMessage);

    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(user.getId(), invalidCode);
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção ao tentar verificar uma conta que já estiver ativa")
  void execute_shouldThrowException_whenAccountIsNotPendingVerification() {
    // Arrange
    User activeUser = createUser(AccountStatus.ACTIVE);
    UUID userId = activeUser.getId();
    var requestDto = new ValidateOtpRequestDto(userId.toString(), defaultOtp);

    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
    doNothing().when(otpPort).validateOtp(activeUser.getId(), defaultOtp);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(requestDto))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Não é possível ativar uma conta que não está pendente de verificação.");

    // Verify
    verify(userRepository, times(1)).findById(userId);
    verify(otpPort, times(1)).validateOtp(activeUser.getId(), defaultOtp);
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }
}

package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.accountverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification.imp.VerifyAccountUseCaseImp;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpSessionException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para VerifyAccountUseCase")
public class VerifyAccountUseCaseTest {

  @Mock private OtpPort otpPort;
  @Mock private AuthPort authPort;
  @Mock private OtpSessionPort otpSessionPort;
  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private VerifyAccountUseCaseImp verifyAccountUseCase;

  private final OtpCode otpCode = OtpCode.generate();
  private final OtpSessionId otpSessionId = OtpSessionId.generate();

  @Test
  @DisplayName("Deve verificar e ativar a conta do usuário com sucesso")
  void execute_shouldCheckAndActivateUserAccount_successfully() {
    // Arrange
    User user = TestDataProvider.createPendintUser();
    UUID userId = user.getId();

    RefreshTokenVO refreshToken = RefreshTokenVO.generate();
    var authResult = new AuthResult(user, "access_token", refreshToken);

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(authPort.generateTokens(user)).thenReturn(authResult);

    // Act
    AuthResult response = verifyAccountUseCase.execute(request);

    // Assert
    ArgumentCaptor<User> userCaptor = forClass(User.class);

    assertThat(response.accessToken()).isEqualTo(authResult.accessToken());
    assertThat(response.refreshToken()).isEqualTo(authResult.refreshToken());

    verify(userRepository, times(1)).save(userCaptor.capture());
    verify(authPort, times(1)).generateTokens(user);

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpSessionException quando a sessão OTP for inválida")
  void execute_shouldThrowInvalidOtpSessionException_whenOtpSessionIsInvalid() {
    // Arrange
    OtpSessionId invalidOtpSessionId = OtpSessionId.generate();
    var request = new ValidateOtpRequestDto(invalidOtpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(invalidOtpSessionId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(InvalidOtpSessionException.class)
        .satisfies(
            ex -> {
              InvalidOtpSessionException exception = (InvalidOtpSessionException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.OTP_SESSION_ID_INCORRECT_OR_EXPIRED);
            });

    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserIsNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException para um código de verificação inválido")
  void execute_shouldThrowInvalidOtpException_forInvalidOtpCode() {
    // Arrange
    User user = TestDataProvider.createPendintUser();
    UUID userId = user.getId();

    OtpCode invalidOtp = OtpCode.generate();
    var request = new ValidateOtpRequestDto(otpSessionId, invalidOtp);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    ErrorCode errorCode = ErrorCode.OTP_CODE_INCORRECT_OR_EXPIRED;
    doThrow(new InvalidOtpException(errorCode)).when(otpPort).validateOtp(user.getId(), invalidOtp);

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(InvalidOtpException.class)
        .satisfies(
            ex -> {
              InvalidOtpException exception = (InvalidOtpException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OTP_CODE_INCORRECT_OR_EXPIRED);
            });

    verify(otpPort, times(1)).validateOtp(user.getId(), invalidOtp);
    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }

  @ParameterizedTest
  @EnumSource(
      value = AccountStatus.class,
      names = {"ACTIVE", "LOCKED", "DISABLED"})
  @DisplayName("Deve retornar AccountStatusForbiddenException quando o status da conta é inválido")
  void execute_shouldThrowAccountStatusForbiddenException_whenAccountStatusIsInvalid(
      AccountStatus invalidStatus) {
    // Arrange
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();
    UUID userId = user.getId();

    var request = new ValidateOtpRequestDto(otpSessionId, otpCode);

    when(otpSessionPort.findUserIdByOtpSessionId(otpSessionId)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> verifyAccountUseCase.execute(request))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.ACCOUNT_NOT_PENDING_VERIFICATION);
            });

    verify(userRepository, never()).save(any(User.class));
    verify(authPort, never()).generateTokens(any(User.class));
  }
}

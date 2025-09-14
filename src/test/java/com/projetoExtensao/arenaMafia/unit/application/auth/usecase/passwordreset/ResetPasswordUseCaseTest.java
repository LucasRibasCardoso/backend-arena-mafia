package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.ResetPasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordHashException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordResetTokenException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ResetPasswordUseCase")
public class ResetPasswordUseCaseTest {

  @Mock private PasswordEncoderPort passwordEncoder;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PasswordResetTokenPort passwordResetToken;
  @InjectMocks private ResetPasswordUseCaseImp resetPasswordUseCase;

  private final ResetToken resetToken = ResetToken.generate();
  private final String newPassword = "newSecurePassword123!";
  private final String confirmPassword = "newSecurePassword123!";

  @Test
  @DisplayName("Deve redefinir a senha do usuário com sucesso")
  public void execute_shouldResetPasswordSuccessfully() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();

    String newPasswordHash = "hashedNewPassword";
    var request = new ResetPasswordRequestDto(resetToken, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(resetToken)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);

    // Act
    resetPasswordUseCase.execute(request);

    // Assert
    ArgumentCaptor<User> userCaptor = forClass(User.class);
    verify(userRepository, times(1)).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo(newPasswordHash);

    verify(passwordResetToken, times(1)).delete(resetToken);
  }

  @Test
  @DisplayName("Deve lançar InvalidPasswordResetTokenException quando o token for inválido")
  public void execute_shouldThrowInvalidPasswordResetTokenException_whenTokenIsInvalid() {
    // Arrange
    var request = new ResetPasswordRequestDto(resetToken, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(resetToken)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(InvalidPasswordResetTokenException.class)
        .satisfies(
            ex -> {
              InvalidPasswordResetTokenException exception =
                  (InvalidPasswordResetTokenException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.RESET_TOKEN_INCORRECT_OR_EXPIRED);
            });

    verify(userRepository, never()).save(any(User.class));
    verify(passwordResetToken, never()).delete(any(ResetToken.class));
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  public void execute_shouldThrowUserNotFoundException_whenUserNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    var request = new ResetPasswordRequestDto(resetToken, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(resetToken)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(userRepository, never()).save(any(User.class));
    verify(passwordResetToken, never()).delete(any(ResetToken.class));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Deve lançar InvalidPasswordHashException quando o hash da senha for inválido")
  public void execute_shouldThrowInvalidPasswordHashException_whenPasswordHashIsInvalid(
      String invalidPasswordHash) {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();
    var request = new ResetPasswordRequestDto(resetToken, confirmPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(resetToken)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode(confirmPassword)).thenReturn(invalidPasswordHash);

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(InvalidPasswordHashException.class)
        .satisfies(
            ex -> {
              InvalidPasswordHashException exception = (InvalidPasswordHashException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_HASH_REQUIRED);
            });
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
  @DisplayName("Deve lançar AccountStatusForbiddenException para status de conta inválidos")
  public void execute_shouldThrowAccountStatusForbiddenException_whenAccountStatusIsInvalid(
      AccountStatus invalidStatus, ErrorCode expectedErrorCode) {
    // Arrange
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();
    UUID userId = user.getId();
    var request = new ResetPasswordRequestDto(resetToken, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(resetToken)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
              assertThat(exception.getMessage()).isEqualTo(expectedErrorCode.getMessage());
            });

    verify(userRepository, never()).save(any(User.class));
    verify(passwordResetToken, never()).delete(any(ResetToken.class));
  }
}

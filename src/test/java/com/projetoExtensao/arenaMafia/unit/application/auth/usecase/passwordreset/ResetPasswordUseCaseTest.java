package com.projetoExtensao.arenaMafia.unit.application.auth.usecase.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.passwordreset.imp.ResetPasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordResetTokenException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResetPasswordRequestDto;
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
@DisplayName("Testes unitários para ResetPasswordUseCase")
public class ResetPasswordUseCaseTest {

  @Mock private PasswordEncoderPort passwordEncoder;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PasswordResetTokenPort passwordResetToken;

  @InjectMocks private ResetPasswordUseCaseImp resetPasswordUseCase;

  private final String token = "valid-reset-token";
  private final String newPassword = "newSecurePassword123!";
  private final String confirmPassword = "newSecurePassword123!";

  private User createUser(AccountStatus accountStatus) {
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "hashedPassword",
        accountStatus,
        RoleEnum.ROLE_USER,
        Instant.now());
  }

  @Test
  @DisplayName("Deve redefinir a senha do usuário com sucesso")
  public void execute_ShouldResetPasswordSuccessfully() {
    // Arrange
    User user = createUser(AccountStatus.ACTIVE);
    String newPasswordHash = "hashedNewPassword";
    var request = new ResetPasswordRequestDto(token, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(token)).thenReturn(Optional.of(user.getId()));

    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);

    // Act
    resetPasswordUseCase.execute(request);

    // Assert
    assertThat(user.getPasswordHash()).isEqualTo(newPasswordHash);

    verify(passwordResetToken, times(1)).findUserIdByResetToken(token);
    verify(userRepository, times(1)).findById(user.getId());
    verify(passwordEncoder, times(1)).encode(newPassword);
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando não encontrar o token de redefinição de senha")
  public void execute_ShouldThrowExceptionForInvalidToken() {
    // Arrange
    var request = new ResetPasswordRequestDto(token, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(token)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(InvalidPasswordResetTokenException.class)
        .hasMessageContaining("Token inválido ou expirado.");

    verify(passwordResetToken, times(1)).findUserIdByResetToken(token);
    verify(userRepository, never()).findById(any());
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando o usuário associado ao token não for encontrado")
  public void execute_ShouldThrowExceptionForUserNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    var request = new ResetPasswordRequestDto(token, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(token)).thenReturn(Optional.of(userId));
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(
            "Ocorreu um erro ao redefinir sua senha. Por favor, inicie o processo novamente.");
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando a conta do usuário estiver bloqueada")
  public void execute_ShouldThrowExceptionForLockedAccount() {
    // Arrange
    User user = createUser(AccountStatus.LOCKED);
    var request = new ResetPasswordRequestDto(token, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(token)).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessageContaining("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");

    verify(passwordResetToken, times(1)).findUserIdByResetToken(token);
    verify(userRepository, times(1)).findById(user.getId());
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando a conta do usuário estiver pendente de verificação")
  public void execute_ShouldThrowExceptionForPendingVerificationAccount() {
    // Arrange
    User user = createUser(AccountStatus.PENDING_VERIFICATION);
    var request = new ResetPasswordRequestDto(token, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(token)).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessageContaining(
            "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");

    verify(passwordResetToken, times(1)).findUserIdByResetToken(token);
    verify(userRepository, times(1)).findById(user.getId());
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("Deve lançar uma exceção quando a conta do usuário estiver desativada")
  public void execute_ShouldThrowExceptionForDisabledAccount() {
    // Arrange
    User user = createUser(AccountStatus.DISABLED);
    String token = "valid-reset-token";
    String newPassword = "newSecurePassword123!";
    String confirmPassword = "newSecurePassword123!";
    var request = new ResetPasswordRequestDto(token, newPassword, confirmPassword);

    when(passwordResetToken.findUserIdByResetToken(token)).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> resetPasswordUseCase.execute(request))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessageContaining("Atenção: Sua conta está desativada.");

    verify(passwordResetToken, times(1)).findUserIdByResetToken(token);
    verify(userRepository, times(1)).findById(user.getId());
    verify(passwordEncoder, never()).encode(any());
  }
}

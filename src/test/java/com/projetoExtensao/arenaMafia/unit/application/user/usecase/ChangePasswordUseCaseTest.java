package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultPassword;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.password.imp.ChangePasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.IncorrectCurrentPasswordException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangePasswordRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
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
@DisplayName("Testes unitários para ChangePasswordUseCase")
public class ChangePasswordUseCaseTest {

  @Mock private PasswordEncoderPort passwordEncoder;
  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private ChangePasswordUseCaseImp changePasswordUseCase;

  @Test
  @DisplayName("Deve alterar a senha do usuário com sucesso")
  public void execute_shouldChangePasswordSuccessfully() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();

    String newPassword = "newPassword123";
    String newPasswordHash = "hashedNewPassword";
    var request = new ChangePasswordRequestDto(defaultPassword, newPassword, newPassword);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(defaultPassword, user.getPasswordHash())).thenReturn(true);
    when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);

    // Act
    changePasswordUseCase.execute(idCurrentUser, request);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    verify(userRepository, times(1)).save(userCaptor.capture());
    verify(passwordEncoder, times(1)).encode(newPassword);

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo(newPasswordHash);
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  public void execute_shouldThrowUserNotFoundException_whenUserNotFound() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    var request = new ChangePasswordRequestDto(defaultPassword, "newPassword", "newPassword");

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> changePasswordUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName(
      "Deve lançar IncorrectCurrentPasswordException quando a senha atual estiver incorreta")
  public void
      execute_shouldThrowIncorrectCurrentPasswordException_whenCurrentPasswordIsIncorrect() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();

    String wrongCurrentPassword = "wrongCurrentPassword";
    var request = new ChangePasswordRequestDto(wrongCurrentPassword, "newPassword", "newPassword");

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(wrongCurrentPassword, user.getPasswordHash())).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> changePasswordUseCase.execute(idCurrentUser, request))
        .isInstanceOf(IncorrectCurrentPasswordException.class)
        .satisfies(
            ex -> {
              IncorrectCurrentPasswordException exception = (IncorrectCurrentPasswordException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_CURRENT_INCORRECT);
            });

    verify(userRepository, never()).save(any(User.class));
  }
}

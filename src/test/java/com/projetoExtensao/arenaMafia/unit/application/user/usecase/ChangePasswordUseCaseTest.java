package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.password.imp.ChangePasswordUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.IncorrectPasswordException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordHashException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangePasswordRequestDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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

  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPhone = "+558320548181";
  private final String defaultPassword = "123456";
  private final String defaultPasswordHash = "hashedOldPassword";

  @Test
  @DisplayName("Deve alterar a senha do usuário com sucesso")
  public void execute_ShouldChangePasswordSuccessfully() {
    // Arrange
    User mockUser =
        User.create(defaultUsername, defaultFullName, defaultPhone, defaultPasswordHash);
    UUID idCurrentUser = mockUser.getId();

    String newPassword = "newPassword123";
    String newPasswordHash = "hashedNewPassword";
    var request = new ChangePasswordRequestDto(defaultPassword, newPassword, newPassword);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches(defaultPassword, defaultPasswordHash)).thenReturn(true);
    when(passwordEncoder.encode(newPassword)).thenReturn(newPasswordHash);

    // Act
    changePasswordUseCase.execute(idCurrentUser, request);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(1)).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    Assertions.assertEquals(newPasswordHash, savedUser.getPasswordHash());

    // Verifica se todos os métodos esperados foram chamados
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(passwordEncoder, times(1)).matches(defaultPassword, defaultPasswordHash);
    verify(passwordEncoder, times(1)).encode(newPassword);
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  public void execute_ShouldThrowException_WhenUserNotFound() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    var request = new ChangePasswordRequestDto(defaultPassword, "newPassword", "newPassword");

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> changePasswordUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("Usuário não encontrado");

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando a senha atual estiver incorreta")
  public void execute_ShouldThrowException_WhenCurrentPasswordIsIncorrect() {
    // Arrange
    User mockUser =
        User.create(defaultUsername, defaultFullName, defaultPhone, defaultPasswordHash);
    UUID idCurrentUser = mockUser.getId();

    String wrongCurrentPassword = "wrongCurrentPassword";
    var request = new ChangePasswordRequestDto(wrongCurrentPassword, "newPassword", "newPassword");

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches(wrongCurrentPassword, defaultPasswordHash)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> changePasswordUseCase.execute(idCurrentUser, request))
        .isInstanceOf(IncorrectPasswordException.class)
        .hasMessage("A Senha atual está incorreta");

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(passwordEncoder, times(1)).matches(wrongCurrentPassword, defaultPasswordHash);
    verify(passwordEncoder, never()).encode(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Deve lançar exceção quando a nova senha for null ou vazia")
  public void execute_ShouldThrowException_WhenNewPasswordHashIsNullOrEmpty(
      String invalidNewPasswordHash) {
    // Arrange
    User mockUser =
        User.create(defaultUsername, defaultFullName, defaultPhone, defaultPasswordHash);
    UUID idCurrentUser = mockUser.getId();

    String validNewPassword = "newPassword123";
    var request = new ChangePasswordRequestDto(defaultPassword, validNewPassword, validNewPassword);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches(defaultPassword, defaultPasswordHash)).thenReturn(true);
    when(passwordEncoder.encode(validNewPassword)).thenReturn(invalidNewPasswordHash);

    // Act & Assert
    assertThatThrownBy(() -> changePasswordUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidPasswordHashException.class)
        .hasMessage("O hash da senha não pode ser nulo ou vazio.");

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(passwordEncoder, times(1)).matches(defaultPassword, defaultPasswordHash);
    verify(passwordEncoder, times(1)).encode(validNewPassword);
    verify(userRepository, never()).save(any(User.class));
  }
}

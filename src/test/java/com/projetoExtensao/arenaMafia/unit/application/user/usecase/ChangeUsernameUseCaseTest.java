package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.username.imp.ChangeUsernameUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUsernameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangeUsernameRequestDto;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ChangeUsernameUseCase")
public class ChangeUsernameUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private ChangeUsernameUseCaseImp changeUsernameUseCase;

  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPhone = "+558320548181";
  private final String defaultPassword = "password123";

  @Test
  @DisplayName("Deve alterar o nome de usuário quando o novo nome de usuário for válido")
  void execute_shouldChangeUsername_whenNewUsernameIsValid() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    String newUsername = "newUsername";
    var request = new ChangeUsernameRequestDto(newUsername);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));
    when(userRepository.findByUsername(newUsername)).thenReturn(Optional.of(mockUser));
    when(userRepository.save(mockUser)).thenReturn(mockUser);

    // Act
    User updateUser = changeUsernameUseCase.execute(idCurrentUser, request);

    // Assert
    assertThat(updateUser.getUsername()).isEqualTo(newUsername);
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(1)).findByUsername(newUsername);
    verify(userRepository, times(1)).save(mockUser);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"ab", "invalid username", "user@name"})
  @DisplayName("Deve lançar exceção quando o novo nome de usuário invalido")
  void execute_shouldThrowException_whenNewUsernameIsInvalid(String invalidUsername) {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    var request = new ChangeUsernameRequestDto(invalidUsername);

    // Act & Assert
    assertThatThrownBy(() -> changeUsernameUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidUsernameFormatException.class);

    verify(userRepository, never()).findById(any(UUID.class));
    verify(userRepository, never()).findByUsername(anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o novo nome de usuário já existir")
  void execute_shouldThrowException_whenNewUsernameAlreadyExists() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = UUID.randomUUID();
    String newUsername = defaultUsername;
    var request = new ChangeUsernameRequestDto(newUsername);

    when(userRepository.findByUsername(newUsername)).thenReturn(Optional.of(mockUser));

    // Act
    assertThatThrownBy(() -> changeUsernameUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessage("Esse nome de usuário já está em uso.");

    // Assert
    verify(userRepository, times(1)).findByUsername(newUsername);
    verify(userRepository, never()).findById(any(UUID.class));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserNotFound() {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    String newUsername = "newUsername";
    var request = new ChangeUsernameRequestDto(newUsername);

    when(userRepository.findByUsername(newUsername)).thenReturn(Optional.of(mockUser));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> changeUsernameUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("Usuário não encontrado.");

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(1)).findByUsername(anyString());
    verify(userRepository, never()).save(any(User.class));
  }
}

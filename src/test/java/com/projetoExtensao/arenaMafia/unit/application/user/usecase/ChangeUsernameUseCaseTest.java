package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultUsername;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.username.imp.ChangeUsernameUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUsernameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangeUsernameRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ChangeUsernameUseCase")
public class ChangeUsernameUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private ChangeUsernameUseCaseImp changeUsernameUseCase;

  @Test
  @DisplayName("Deve alterar o nome de usuário quando o novo nome de usuário for válido")
  void execute_shouldChangeUsername_whenNewUsernameIsValid() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    var request = new ChangeUsernameRequestDto("newUsername");

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));
    when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    // Act
    changeUsernameUseCase.execute(idCurrentUser, request);
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    verify(userRepository, times(1)).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getUsername()).isEqualTo(request.username());
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidUsernameProvider")
  @DisplayName("Deve lançar InvalidUsernameFormatException quando o novo nome de usuário invalido")
  void execute_shouldThrowException_whenNewUsernameIsInvalid(
      String invalidUsername, ErrorCode expectedErrorCode) {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    var request = new ChangeUsernameRequestDto(invalidUsername);

    when(userRepository.findByUsername(invalidUsername)).thenReturn(Optional.of(user));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> changeUsernameUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidUsernameFormatException.class)
        .satisfies(
            ex -> {
              InvalidUsernameFormatException exception = (InvalidUsernameFormatException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
            });

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar UserAlreadyExistsException quando o novo nome de usuário já existir")
  void execute_shouldThrowException_whenNewUsernameAlreadyExists() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = UUID.randomUUID();

    var request = new ChangeUsernameRequestDto(defaultUsername);

    when(userRepository.findByUsername(defaultUsername)).thenReturn(Optional.of(user));

    // Act
    assertThatThrownBy(() -> changeUsernameUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .satisfies(
            ex -> {
              UserAlreadyExistsException exception = (UserAlreadyExistsException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
            });

    // Assert
    verify(userRepository, times(1)).findByUsername(defaultUsername);
    verify(userRepository, never()).findById(any(UUID.class));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserNotFound() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    var request = new ChangeUsernameRequestDto("newUsername");

    when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(user));
    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> changeUsernameUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(userRepository, never()).save(any(User.class));
  }
}

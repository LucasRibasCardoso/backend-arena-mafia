package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.imp.UpdateProfileUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatFullNameException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.UpdateProfileRequestDto;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DisplayName("Testes unitários para UpdateProfileUseCase")
public class UpdateProfileUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private UpdateProfileUseCaseImp updateProfileUseCase;

  @Test
  @DisplayName("Deve atualizar o perfil do usuário quando os dados forem válidos")
  void execute_shouldUpdateUserProfile_whenDataIsValid() {
    // Arrange
    String newFullName = "Updated User";
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    var request = new UpdateProfileRequestDto(newFullName);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    // Act
    User updateUser = updateProfileUseCase.execute(idCurrentUser, request);

    // Assert
    assertThat(updateUser.getFullName()).isEqualTo(newFullName);
    verify(userRepository, times(1)).save(user);
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserNotFound() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    var request = new UpdateProfileRequestDto("Updated User");

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> updateProfileUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(userRepository, never()).save(any(User.class));
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidFullNameProvider")
  @DisplayName("Deve lançar exceção quando o nome completo for inválido")
  void execute_shouldThrowException_whenFullNameIsInvalid(
      String invalidFullName, ErrorCode expectedErrorCode) {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    var request = new UpdateProfileRequestDto(invalidFullName);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> updateProfileUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidFormatFullNameException.class)
        .satisfies(
            ex -> {
              InvalidFormatFullNameException exception = (InvalidFormatFullNameException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
            });

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, never()).save(any(User.class));
  }
}

package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.imp.UpdateProfileUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFullNameException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.UpdateProfileRequestDTO;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DisplayName("Testes unitários para UpdateProfileUseCase")
public class UpdateProfileUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private UpdateProfileUseCaseImp updateProfileUseCase;

  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPhone = "+558320548181";
  private final String defaultPassword = "password123";

  @Test
  @DisplayName("Deve atualizar o perfil do usuário quando os dados forem válidos")
  void execute_shouldUpdateUserProfile_whenDataIsValid() {
    // Arrange
    String newFullName = "Updated User";
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    var request = new UpdateProfileRequestDTO(newFullName);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));
    when(userRepository.save(mockUser)).thenReturn(mockUser);

    // Act
    User updateUser = updateProfileUseCase.execute(idCurrentUser, request);

    // Assert
    assertThat(updateUser.getFullName()).isEqualTo(newFullName);
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(1)).save(mockUser);
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserNotFound() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();
    var request = new UpdateProfileRequestDTO("Updated User");

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> updateProfileUseCase.execute(idCurrentUser, request))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("Usuário não encontrado.");

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(0)).save(any(User.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ab", "  "})
  @DisplayName("Deve lançar exceção quando o nome completo for inválido")
  void execute_shouldThrowException_whenFullNameIsInvalid(String invalidFullName) {
    // Arrange
    User mockUser = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);
    UUID idCurrentUser = mockUser.getId();
    var request = new UpdateProfileRequestDTO(invalidFullName);

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(mockUser));

    // Act & Assert
    assertThatThrownBy(() -> updateProfileUseCase.execute(idCurrentUser, request))
        .isInstanceOf(InvalidFullNameException.class);

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, never()).save(any(User.class));
  }
}

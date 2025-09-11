package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.profile.imp.GetUserProfileUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para GetUserProfileUseCase")
public class GetUserProfileUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private GetUserProfileUseCaseImp getUserProfileUseCase;

  @Test
  @DisplayName("Deve retornar os dados do perfil do usuário com sucesso")
  void execute_shouldReturnUserProfileSuccessfully() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    // Act
    User response = getUserProfileUseCase.execute(user.getId());

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário não for encontrado")
  void execute_shouldThrowUserNotFoundException_whenUserNotFound() {
    // Arrange
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> getUserProfileUseCase.execute(userId))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });
  }
}

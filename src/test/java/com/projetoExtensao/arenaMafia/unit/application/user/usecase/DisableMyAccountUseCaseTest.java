package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.disable.imp.DisableMyAccountUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para DisableMyAccountUseCase")
public class DisableMyAccountUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private DisableMyAccountUseCaseImp disableMyAccountUseCase;

  private final RoleEnum defaultRole = RoleEnum.ROLE_USER;

  private User mockUser(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        "testuser",
        "Test User",
        "+558320548181",
        "password123",
        accountStatus,
        defaultRole,
        now,
        now);
  }

  @Test
  @DisplayName("Deve desativar a conta do usuário com sucesso")
  void shouldDisableUserAccountSuccessfully() {
    // Arrange
    User user = mockUser(AccountStatus.ACTIVE);
    UUID idCurrentUser = user.getId();

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    // Act
    disableMyAccountUseCase.execute(idCurrentUser);

    // Assert
    assertThat(user.isEnabled()).isFalse();
    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, times(1)).save(user);
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserNotFound() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> disableMyAccountUseCase.execute(idCurrentUser))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("Usuário não encontrado");

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, never()).save(any(User.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"LOCKED", "DISABLED", "PENDING_VERIFICATION"})
  @DisplayName("Deve lançar exceção quando o status da conta não for ACTIVE")
  void execute_shouldThrowException_whenAccountStatusIsNotActive(String status) {
    // Arrange
    AccountStatus accountStatus = AccountStatus.valueOf(status);
    User user = mockUser(accountStatus);
    UUID idCurrentUser = user.getId();

    when(userRepository.findById(idCurrentUser)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThatThrownBy(() -> disableMyAccountUseCase.execute(idCurrentUser))
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Sua conta precisa estar ativa para ser desativada.");

    verify(userRepository, times(1)).findById(idCurrentUser);
    verify(userRepository, never()).save(any(User.class));
  }
}

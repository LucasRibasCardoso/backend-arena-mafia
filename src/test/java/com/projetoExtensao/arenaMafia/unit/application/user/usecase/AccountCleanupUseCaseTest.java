package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.cleanup.imp.AccountCleanupUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para AccountCleanupUseCase")
public class AccountCleanupUseCaseTest {

  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
  @Mock private UserRepositoryPort userRepository;
  @InjectMocks private AccountCleanupUseCaseImp accountCleanupUseCase;

  @Test
  @DisplayName("Deve deletar usuários pendentes e seus tokens quando encontrados")
  void executeCleanupOfPendingAccounts_shouldDeleteUsersAndTokens_whenAccountsAreFound() {
    // Arrange
    List<User> fakeUsersToClean = List.of(mock(User.class), mock(User.class));

    when(userRepository.findByStatusAndCreatedAtBefore(
            eq(AccountStatus.PENDING_VERIFICATION), any(Instant.class)))
        .thenReturn(fakeUsersToClean);

    // Act
    accountCleanupUseCase.executeCleanupOfPendingAccounts();

    // Assert
    verify(refreshTokenRepository, times(1)).deleteAllByUser(fakeUsersToClean);
    verify(userRepository, times(1)).deleteAll(fakeUsersToClean);
  }

  @Test
  @DisplayName("Não deve chamar métodos de deleção se nenhuma conta pendente for encontrada")
  void executeCleanupOfPendingAccounts_shouldDoNothing_whenNoAccountsAreFound() {
    // Arrange
    when(userRepository.findByStatusAndCreatedAtBefore(
            eq(AccountStatus.PENDING_VERIFICATION), any(Instant.class)))
        .thenReturn(Collections.emptyList());

    // Act
    accountCleanupUseCase.executeCleanupOfPendingAccounts();

    // Assert
    verify(refreshTokenRepository, never()).deleteAllByUser(anyList());
    verify(userRepository, never()).deleteAll(anyList());
  }

  @Test
  @DisplayName("Deve deletar APENAS usuários desativados quando encontrados")
  void executeCleanupOfDisabledAccounts_shouldDeleteOnlyUsers_whenAccountsAreFound() {
    // Arrange
    List<User> fakeUsersToClean = List.of(mock(User.class));
    when(userRepository.findByStatusAndUpdateAtBefore(
            eq(AccountStatus.DISABLED), any(Instant.class)))
        .thenReturn(fakeUsersToClean);

    // Act
    accountCleanupUseCase.executeCleanupOfDisabledAccounts();

    // Assert
    verify(refreshTokenRepository, times(1)).deleteAllByUser(anyList());
    verify(userRepository, times(1)).deleteAll(fakeUsersToClean);
  }

  @Test
  @DisplayName("Não deve chamar métodos de deleção se nenhuma conta desativada for encontrada")
  void executeCleanupOfDisabledAccounts_shouldDoNothing_whenNoAccountsAreFound() {
    // Arrange
    when(userRepository.findByStatusAndUpdateAtBefore(
            eq(AccountStatus.DISABLED), any(Instant.class)))
        .thenReturn(Collections.emptyList());

    // Act
    accountCleanupUseCase.executeCleanupOfDisabledAccounts();

    // Assert
    verify(refreshTokenRepository, never()).deleteAllByUser(anyList());
    verify(userRepository, never()).deleteAll(anyList());
  }
}

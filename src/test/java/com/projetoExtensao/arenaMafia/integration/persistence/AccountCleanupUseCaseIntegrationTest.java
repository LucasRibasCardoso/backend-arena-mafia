package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.cleanup.imp.AccountCleanupUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.integration.config.BaseTestContainersConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração para AccountCleanupUseCase")
public class AccountCleanupUseCaseIntegrationTest extends BaseTestContainersConfig {

  @Autowired private RefreshTokenRepositoryPort refreshTokenRepository;
  @Autowired private UserRepositoryPort userRepositoryPort;
  @Autowired private AccountCleanupUseCaseImp accountCleanupUseCase;

  @Test
  @DisplayName("Deve deletar usuários pendentes e seus tokens quando encontrados")
  void executeCleanupOfPendingAccounts_shouldDeleteUsersAndTokens_whenAccountsAreFound() {
    // Arrange
    User userToDelete =
        mockPersistUser(
            "userToDelete",
            "+551100000001",
            AccountStatus.PENDING_VERIFICATION,
            Instant.now().minus(25, ChronoUnit.HOURS),
            Instant.now().minus(25, ChronoUnit.HOURS));
    RefreshToken token = refreshTokenRepository.save(RefreshToken.create(7L, userToDelete));

    User mockRecentPendingUser =
        mockPersistUser(
            "mockRecentPendingUser",
            "+551100000002",
            AccountStatus.PENDING_VERIFICATION,
            Instant.now().minus(1, ChronoUnit.HOURS),
            Instant.now().minus(1, ChronoUnit.HOURS));

    User mockActivedUser = mockPersistUser();

    // Act
    accountCleanupUseCase.executeCleanupOfPendingAccounts();

    // Assert
    assertThat(userRepositoryPort.findById(userToDelete.getId())).isEmpty();
    assertThat(refreshTokenRepository.findByToken(token.getToken())).isEmpty();

    assertThat(userRepositoryPort.findById(mockRecentPendingUser.getId())).isPresent();
    assertThat(userRepositoryPort.findById(mockActivedUser.getId())).isPresent();
  }

  @Test
  @DisplayName("Deve deletar usuários desativados e seus tokens quando encontrados")
  void executeCleanupOfDisabledAccounts_shouldDeleteUsersAndTokens_whenAccountsAreFound() {
    // Arrange
    User userToDelete =
        mockPersistUser(
            "userToDelete",
            "+551100000001",
            AccountStatus.DISABLED,
            Instant.now().minus(10, ChronoUnit.DAYS),
            Instant.now().minus(8, ChronoUnit.DAYS));
    RefreshToken token = refreshTokenRepository.save(RefreshToken.create(7L, userToDelete));

    User mockRecentDisabledUser =
        mockPersistUser(
            "mockRecentDisabledUser",
            "+551100000002",
            AccountStatus.DISABLED,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS));

    User mockActivedUser = mockPersistUser();

    // Act
    accountCleanupUseCase.executeCleanupOfDisabledAccounts();

    // Assert
    assertThat(userRepositoryPort.findById(userToDelete.getId())).isEmpty();
    assertThat(refreshTokenRepository.findByToken(token.getToken())).isEmpty();

    assertThat(userRepositoryPort.findById(mockRecentDisabledUser.getId())).isPresent();
    assertThat(userRepositoryPort.findById(mockActivedUser.getId())).isPresent();
  }
}

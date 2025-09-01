package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUsernameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@DisplayName("Testes unitários para entidade User")
public class UserTest {

  private final String defaultUsername = "testuser";
  private final String defaultFullName = "Test User";
  private final String defaultPhone = "+558320548181";
  private final String defaultPasswordHash = "hashedPassword";

  private User createUser(AccountStatus accountStatus) {
    return User.reconstitute(
        UUID.randomUUID(),
        defaultUsername,
        defaultFullName,
        defaultPhone,
        defaultPasswordHash,
        accountStatus,
        RoleEnum.ROLE_USER,
        Instant.now());
  }

  @Nested
  @DisplayName("Sucesso ao criar um usuário")
  class SuccessScenarios {

    @Test
    @DisplayName("Deve criar um usuário com todos os valores padrões e corretos")
    void create_shouldCreateUserSuccessfully() {
      // Act
      User user = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPasswordHash);

      // Assert
      assertThat(user.getId().toString()).hasSize(36); // UUID padrão
      assertThat(user.getUsername()).isEqualTo(defaultUsername);
      assertThat(user.getFullName()).isEqualTo(defaultFullName);
      assertThat(user.getPhone()).isEqualTo(defaultPhone);
      assertThat(user.getPasswordHash()).isEqualTo(defaultPasswordHash);
      assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
      assertThat(user.getCreatedAt()).isBefore(Instant.now());
    }

    @Test
    void reconstitute_shouldReconstructUserSuccessfully() {
      // Act
      User user =
          User.reconstitute(
              UUID.randomUUID(),
              defaultUsername,
              defaultFullName,
              defaultPhone,
              defaultPasswordHash,
              AccountStatus.PENDING_VERIFICATION,
              RoleEnum.ROLE_USER,
              Instant.now());

      // Assert
      assertThat(user.getId().toString()).hasSize(36); // UUID padrão
      assertThat(user.getUsername()).isEqualTo(defaultUsername);
      assertThat(user.getFullName()).isEqualTo(defaultFullName);
      assertThat(user.getPhone()).isEqualTo(defaultPhone);
      assertThat(user.getPasswordHash()).isEqualTo(defaultPasswordHash);
      assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
      assertThat(user.getCreatedAt()).isBefore(Instant.now());
    }
  }

  @Nested
  @DisplayName("Falhas ao tentar criar um usuário inválido")
  class FailureScenarios {

    @ParameterizedTest(name = "Deve lançar exceção para username: \"{0}\"")
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidUsernameProvider")
    @DisplayName(
        "Deve lançar InvalidUsernameFormatException para usernames inválidos ao tentar criar um usuário")
    void create_shouldThrowException_whenUsernameIsInvalid(
        String invalidUsername, String expectMessage) {

      // Arrange & Act & Assert
      assertThatThrownBy(
              () ->
                  User.create(invalidUsername, defaultFullName, defaultPhone, defaultPasswordHash))
          .isInstanceOf(InvalidUsernameFormatException.class)
          .hasMessage(expectMessage);
    }

    @ParameterizedTest(name = "Deve lançar uma exceção para fullName: \"{0}\"")
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidUsernameProvider")
    @DisplayName(
        "Deve lançar InvalidUsernameFormatException para usernames inválidos ao tentar reconstituir um usuário")
    void reconstitute_shouldThrowException_whenUsernameIsInvalid(
        String invalidUsername, String expectMessage) {

      // Arrange & Act & Assert
      assertThatThrownBy(
              () ->
                  User.reconstitute(
                      UUID.randomUUID(),
                      invalidUsername,
                      defaultFullName,
                      defaultPhone,
                      defaultPasswordHash,
                      AccountStatus.ACTIVE,
                      RoleEnum.ROLE_USER,
                      Instant.now()))
          .isInstanceOf(InvalidUsernameFormatException.class)
          .hasMessage(expectMessage);
    }
  }

  @Nested
  @DisplayName("Gerenciamento da conta")
  class AccountManagementTests {

    @Test
    @DisplayName("activateAccount() deve ativar a conta se o status for PENDING_VERIFICATION")
    void activateAccount_shouldActivateAccount_whenPendingVerification() {
      // Arrange
      User user = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPasswordHash);

      // Act
      user.activateAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "LOCKED", "DISABLED"})
    @DisplayName(
        "activateAccount() deve lançar exceção quando o status não for PENDING_VERIFICATION")
    void activateAccount_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = createUser(invalidStatus);

      // Act & Assert
      assertThatThrownBy(user::activateAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está ativada. Você pode fazer login.");
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"PENDING_VERIFICATION", "ACTIVE", "DISABLED"})
    @DisplayName("lockAccount() deve bloquear a conta se ela não estiver bloqueada")
    void lockAccount_shouldLockAccount_whenStatusIsValid(AccountStatus status) {
      // Arrange
      User user = createUser(status);

      // Act
      user.lockAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    @DisplayName("lockAccount() deve lançar exceção se a conta já estiver bloqueada")
    void lockAccount_shouldThrowException_whenStatusIsLocked() {
      // Arrange
      User user = createUser(AccountStatus.LOCKED);

      // Act & Assert
      assertThatThrownBy(user::lockAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está bloqueada.");
    }

    @Test
    @DisplayName("unlockAccount() deve ativar a conta se o status for LOCKED")
    void unlockAccount_shouldActivateAccount_whenStatusIsLocked() {
      // Arrange
      User user = createUser(AccountStatus.LOCKED);

      // Act
      user.unlockAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "PENDING_VERIFICATION", "DISABLED"})
    @DisplayName("unlockAccount() deve lançar exceção para status inválidos")
    void unlockAccount_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = createUser(invalidStatus);

      // Act & Assert
      assertThatThrownBy(user::unlockAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está desbloqueada.");
    }
  }

  @Nested
  @DisplayName("Verificação de status da conta")
  class AccountStatusCheckTests {

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    @DisplayName("isEnabled() deve retornar true apenas para o status ACTIVE")
    void isEnabled_shouldReturnTrue_whenAccountIsActive(AccountStatus status) {
      // Arrange
      User user = createUser(status);

      // Act
      boolean result = user.isEnabled();

      // Assert
      assertThat(result).isEqualTo(status == AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    @DisplayName("isAccountNonLocked() deve retornar true para status diferentes de LOCKED")
    void isAccountNonLocked_shouldReturnTrueForNonLockedStatus(AccountStatus status) {
      // Arrange
      User user = createUser(status);

      // Act
      boolean result = user.isAccountNonLocked();

      // Assert
      assertThat(result).isEqualTo(status != AccountStatus.LOCKED);
    }
  }

  @Nested
  @DisplayName("Permissões de usuários")
  class UserRoleTest {

    private User createUserWithRole(RoleEnum role) {
      return User.reconstitute(
          UUID.randomUUID(),
          defaultUsername,
          defaultFullName,
          defaultPhone,
          defaultPasswordHash,
          AccountStatus.ACTIVE,
          role,
          Instant.now());
    }

    @ParameterizedTest
    @EnumSource(RoleEnum.class)
    @DisplayName("isAdmin() deve retornar true apenas para usuários com a permissão ADMIN")
    void isAdmin_shouldReturnTrue_whenRoleIsAdmin(RoleEnum role) {
      // Arrange
      User user = createUserWithRole(role);

      // Act
      boolean result = user.isAdmin();

      // Assert
      assertThat(result).isEqualTo(role == RoleEnum.ROLE_ADMIN);
    }

    @ParameterizedTest
    @EnumSource(RoleEnum.class)
    @DisplayName("isManager() deve retornar true apenas para usuários com a permissão MANAGER")
    void isManager_shouldReturnTrue_whenRoleIsManager(RoleEnum role) {
      // Arrange
      User user = createUserWithRole(role);

      // Act
      boolean result = user.isManager();

      // Assert
      assertThat(result).isEqualTo(role == RoleEnum.ROLE_MANAGER);
    }

    @ParameterizedTest
    @EnumSource(RoleEnum.class)
    @DisplayName("isUser() deve retornar true apenas para usuários com a permissão USER")
    void isUser_shouldReturnTrue_whenRoleIsUser(RoleEnum roleToTest) {
      // Arrange
      User user = createUserWithRole(roleToTest);

      // Act
      boolean result = user.isUser();

      // Assert
      assertThat(result).isEqualTo(roleToTest == RoleEnum.ROLE_USER);
    }
  }

  // Define o valor invalido para o username e a respectiva mensagem de erro que será lançada
  private static Stream<Arguments> invalidUsernameProvider() {
    return Stream.of(
        Arguments.of(null, "O nome de usuário não pode ser nulo ou vazio."),
        Arguments.of("", "O nome de usuário não pode ser nulo ou vazio."),
        Arguments.of(" ", "O nome de usuário não pode ser nulo ou vazio."),
        Arguments.of("user with spaces", "O nome de usuário não pode conter espaços."),
        Arguments.of("usr", "O nome de usuário deve ter entre 4 e 50 caracteres."),
        Arguments.of("u".repeat(51), "O nome de usuário deve ter entre 4 e 50 caracteres."));
  }
}

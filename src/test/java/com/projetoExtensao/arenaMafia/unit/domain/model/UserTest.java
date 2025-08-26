package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUsernameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@DisplayName("Testes unitários para entidade User")
public class UserTest {

  // Dados padrões para entidade User
  private String username;
  private String fullName;
  private String phone;
  private String passwordHash;

  @BeforeEach
  public void setup() {
    // Inicializa os dados padrão para a entidade User, para evitar repetição em cada teste
    username = "testUser";
    fullName = "Test User";
    phone = "55479123456789";
    passwordHash = "hashedPassword123";
  }

  @Nested
  @DisplayName("Sucesso ao criar um usuário")
  class SuccessScenarios {
    @Test
    @DisplayName("Deve criar um usuário com todos os valores padrão e dados de entrada corretos")
    void create_shouldCreateUserWithCorrectDefaults() {
      // Act
      User newUser = User.create(username, fullName, phone, passwordHash);

      // Assert
      assertThat(newUser).isNotNull();
      assertThat(newUser.getUsername()).isEqualTo(username);
      assertThat(newUser.getFullName()).isEqualTo(fullName);
      assertThat(newUser.getPhone()).isEqualTo(phone);
      assertThat(newUser.getPasswordHash()).isEqualTo(passwordHash);
      assertThat(newUser.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      assertThat(newUser.isEnabled()).isFalse();
      assertThat(newUser.isAccountNonLocked()).isTrue();
      assertNotNull(newUser.getId());
      assertNotNull(newUser.getCreatedAt());
    }

    @Test
    void reconstitute_shouldReconstructUserFromExistingData() {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant createdAt = Instant.now();

      // Act
      User user =
          User.reconstitute(
              id,
              username,
              fullName,
              phone,
              passwordHash,
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_USER,
              createdAt);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getId()).isEqualTo(id);
      assertThat(user.getUsername()).isEqualTo(username);
      assertThat(user.getFullName()).isEqualTo(fullName);
      assertThat(user.getPhone()).isEqualTo(phone);
      assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
      assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      assertThat(user.isEnabled()).isTrue();
      assertThat(user.isAccountNonLocked()).isTrue();
      assertThat(user.getCreatedAt()).isEqualTo(createdAt);
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
    void create_shouldThrowExceptionForInvalidUsernames(
        String invalidUsername, String expectMessage) {

      // Arrange & Act & Assert
      assertThatThrownBy(() -> User.create(invalidUsername, fullName, phone, passwordHash))
          .isInstanceOf(InvalidUsernameFormatException.class)
          .hasMessage(expectMessage);
    }

    @ParameterizedTest(name = "Deve lançar uma exceção para fullName: \"{0}\"")
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidUsernameProvider")
    @DisplayName(
        "Deve lançar InvalidUsernameFormatException para usernames inválidos ao tentar reconstituir um usuário")
    void reconstitute_shouldThrowExceptionForInvalidFullName(
        String invalidUsername, String expectMessage) {
      // Arrange
      UUID id = UUID.randomUUID();
      Instant createdAt = Instant.now();

      // Act & Assert
      assertThatThrownBy(
              () ->
                  User.reconstitute(
                      id,
                      invalidUsername,
                      fullName,
                      phone,
                      passwordHash,
                      AccountStatus.ACTIVE,
                      RoleEnum.ROLE_USER,
                      createdAt))
          .isInstanceOf(InvalidUsernameFormatException.class)
          .hasMessage(expectMessage);
    }
  }

  @Nested
  @DisplayName("Gerenciamento da conta")
  class AccountManagementTests {

    @Test
    @DisplayName("activateAccount() deve ativar a conta se o status for PENDING_VERIFICATION")
    void activateAccount_shouldActivateAccountWhenPendingVerification() {
      // Arrange
      User user = User.create(username, fullName, phone, passwordHash);
      assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);

      // Act
      user.activateAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "LOCKED"})
    @DisplayName("activateAccount() deve lançar exceção para status inválidos")
    void activateAccount_shouldThrowExceptionForInvalidStatus(AccountStatus invalidStatus) {
      // Arrange
      User user =
          User.reconstitute(
              UUID.randomUUID(),
              username,
              fullName,
              phone,
              passwordHash,
              invalidStatus,
              RoleEnum.ROLE_USER,
              Instant.now());

      // Act & Assert
      assertThatThrownBy(user::activateAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está ativada.");
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"PENDING_VERIFICATION", "ACTIVE"})
    @DisplayName("lockAccount() deve bloquear a conta de status PENDING_VERIFICATION ou ACTIVE")
    void lockAccount_shouldLockAccountFromValidStates(AccountStatus initialState) {
      // Arrange
      User user =
          User.reconstitute(
              UUID.randomUUID(),
              username,
              fullName,
              phone,
              passwordHash,
              initialState,
              RoleEnum.ROLE_USER,
              Instant.now());

      // Act
      user.lockAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    @DisplayName("lockAccount() deve lançar exceção se a conta já estiver bloqueada")
    void lockAccount_shouldThrowExceptionWhenAlreadyLocked() {
      // Arrange
      User user =
          User.reconstitute(
              UUID.randomUUID(),
              username,
              fullName,
              phone,
              passwordHash,
              AccountStatus.LOCKED,
              RoleEnum.ROLE_USER,
              Instant.now());

      // Act & Assert
      assertThatThrownBy(user::lockAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está bloqueada.");
    }

    @Test
    @DisplayName("unlockAccount() deve ativar a conta se o status for LOCKED")
    void unlockAccount_shouldActivateAccountWhenLocked() {
      // Arrange
      User user =
          User.reconstitute(
              UUID.randomUUID(),
              username,
              fullName,
              phone,
              passwordHash,
              AccountStatus.LOCKED,
              RoleEnum.ROLE_USER,
              Instant.now());

      // Act
      user.unlockAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "PENDING_VERIFICATION"})
    @DisplayName("unlockAccount() deve lançar exceção para status inválidos")
    void unlockAccount_shouldThrowExceptionForInvalidStatus(AccountStatus invalidStatus) {
      // Arrange
      User user =
          User.reconstitute(
              UUID.randomUUID(),
              username,
              fullName,
              phone,
              passwordHash,
              invalidStatus,
              RoleEnum.ROLE_USER,
              Instant.now());

      // Act & Assert
      assertThatThrownBy(user::unlockAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta não está bloqueada.");
    }
  }

  @Nested
  @DisplayName("Verificação de status da conta")
  class AccountStatusCheckTests {
    private User createUserWithStatus(AccountStatus status) {
      return User.reconstitute(
          UUID.randomUUID(),
          username,
          fullName,
          phone,
          passwordHash,
          status,
          RoleEnum.ROLE_USER,
          Instant.now());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    @DisplayName("isEnabled() deve retornar true apenas para o status ACTIVE")
    void isEnabled_shouldReturnTrueOnlyForActiveStatus(AccountStatus statusToTest) {
      // Arrange
      User user = createUserWithStatus(statusToTest);

      // Act
      boolean result = user.isEnabled();

      // Assert
      assertThat(result).isEqualTo(statusToTest == AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    @DisplayName("isAccountNonLocked() deve retornar true para status diferentes de LOCKED")
    void isAccountNonLocked_shouldReturnTrueForNonLockedStatus(AccountStatus statusToTest) {
      // Arrange
      User user = createUserWithStatus(statusToTest);

      // Act
      boolean result = user.isAccountNonLocked();

      // Assert
      assertThat(result).isEqualTo(statusToTest != AccountStatus.LOCKED);
    }
  }

  @Nested
  @DisplayName("Permissões de usuários")
  class UserRoleTest {

    private User createUserWithRole(RoleEnum role) {
      return User.reconstitute(
          UUID.randomUUID(),
          username,
          fullName,
          phone,
          passwordHash,
          AccountStatus.ACTIVE,
          role,
          Instant.now());
    }

    @ParameterizedTest
    @EnumSource(RoleEnum.class)
    @DisplayName("isAdmin() deve retornar true apenas para usuários com a permissão ADMIN")
    void isAdmin_shouldReturnTrueForAdminRole(RoleEnum roleToTest) {
      // Arrange
      User user = createUserWithRole(roleToTest);

      // Act
      boolean result = user.isAdmin();

      // Assert
      assertThat(result).isEqualTo(roleToTest == RoleEnum.ROLE_ADMIN);
    }

    @ParameterizedTest
    @EnumSource(RoleEnum.class)
    @DisplayName("isManager() deve retornar true apenas para usuários com a permissão MANAGER")
    void isManager_shouldReturnTrueForManagerRole(RoleEnum roleToTest) {
      // Arrange
      User user = createUserWithRole(roleToTest);

      // Act
      boolean result = user.isManager();

      // Assert
      assertThat(result).isEqualTo(roleToTest == RoleEnum.ROLE_MANAGER);
    }

    @ParameterizedTest
    @EnumSource(RoleEnum.class)
    @DisplayName("isUser() deve retornar true apenas para usuários com a permissão USER")
    void isUser_shouldReturnTrueForUserRole(RoleEnum roleToTest) {
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

package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFullNameException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordHashException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPhoneException;
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

  private User createDefaultUser() {
    return User.create(defaultUsername, defaultFullName, defaultPhone, defaultPasswordHash);
  }

  private User createUserWithStatus(AccountStatus accountStatus) {
    Instant now = Instant.now();
    return User.reconstitute(
        UUID.randomUUID(),
        defaultUsername,
        defaultFullName,
        defaultPhone,
        defaultPasswordHash,
        accountStatus,
        RoleEnum.ROLE_USER,
        now,
        now);
  }

  @Nested
  @DisplayName("Testes para os Factory Methods")
  class FactoryMethodTests {

    @Nested
    @DisplayName("Cenários de Sucesso")
    class Success {
      @Test
      @DisplayName("create() deve criar um usuário com valores padrão corretos")
      void create_shouldCreateUserSuccessfully() {
        // Act
        User user = createDefaultUser();

        // Assert
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo(defaultUsername);
        assertThat(user.getFullName()).isEqualTo(defaultFullName);
        assertThat(user.getPhone()).isEqualTo(defaultPhone);
        assertThat(user.getPasswordHash()).isEqualTo(defaultPasswordHash);
        assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_USER);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(user.getCreatedAt()).isNotNull();
      }
    }

    @Nested
    @DisplayName("Cenários de Falha (Validações)")
    class Failure {
      @ParameterizedTest(name = "username: \"{0}\"")
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidUsernameProvider")
      @DisplayName("create() deve lançar exceção para usernames inválidos")
      void create_shouldThrowException_whenUsernameIsInvalid(
          String invalidUsername, String expectedMessage) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () ->
                    User.create(
                        invalidUsername, defaultFullName, defaultPhone, defaultPasswordHash))
            .isInstanceOf(InvalidUsernameFormatException.class)
            .hasMessage(expectedMessage);
      }

      @ParameterizedTest(name = "fullName: \"{0}\"")
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidFullNameProvider")
      @DisplayName("create() deve lançar exceção para fullNames inválidos")
      void create_shouldThrowException_whenFullNameIsInvalid(
          String invalidFullName, String expectedMessage) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () ->
                    User.create(
                        defaultUsername, invalidFullName, defaultPhone, defaultPasswordHash))
            .isInstanceOf(InvalidFullNameException.class)
            .hasMessage(expectedMessage);
      }

      @ParameterizedTest(name = "phone: \"{0}\"")
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidPhoneProvider")
      @DisplayName("create() deve lançar exceção para phones inválidos")
      void create_shouldThrowException_whenPhoneIsInvalid(
          String invalidPhone, String expectedMessage) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () ->
                    User.create(
                        defaultUsername, defaultFullName, invalidPhone, defaultPasswordHash))
            .isInstanceOf(InvalidPhoneException.class)
            .hasMessage(expectedMessage);
      }

      @ParameterizedTest(name = "passwordHash: \"{0}\"")
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidPasswordHashProvider")
      @DisplayName("create() deve lançar exceção para passwordHashes inválidos")
      void create_shouldThrowException_whenPasswordHashIsInvalid(
          String invalidPasswordHash, String expectedMessage) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () ->
                    User.create(
                        defaultUsername, defaultFullName, defaultPhone, invalidPasswordHash))
            .isInstanceOf(InvalidPasswordHashException.class)
            .hasMessage(expectedMessage);
      }
    }
  }

  @Nested
  @DisplayName("Testes para os Métodos de Atualização (update...)")
  class AttributeUpdateTests {

    @Test
    @DisplayName("updateUsername() deve alterar o username com um valor válido")
    void updateUsername_shouldUpdateUsername_whenValid() {
      // Arrange
      User user = createDefaultUser();
      String newUsername = "new_valid_user";

      // Act
      user.updateUsername(newUsername);

      // Assert
      assertThat(user.getUsername()).isEqualTo(newUsername);
    }

    @ParameterizedTest(name = "username: \"{0}\"")
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidUsernameProvider")
    @DisplayName("updateUsername() deve lançar exceção para valores inválidos")
    void updateUsername_shouldThrowException_whenInvalid(
        String invalidUsername, String expectedMessage) {
      // Arrange
      User user = createDefaultUser();

      // Act & Assert
      assertThatThrownBy(() -> user.updateUsername(invalidUsername))
          .isInstanceOf(InvalidUsernameFormatException.class)
          .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("updateFullName() deve alterar o fullName com um valor válido")
    void updateFullName_shouldUpdateFullName_whenValid() {
      // Arrange
      User user = createDefaultUser();
      String newFullName = "New Valid Name";

      // Act
      user.updateFullName(newFullName);

      // Assert
      assertThat(user.getFullName()).isEqualTo(newFullName);
    }

    @ParameterizedTest(name = "fullName: \"{0}\"")
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidFullNameProvider")
    @DisplayName("updateFullName() deve lançar exceção para valores inválidos")
    void updateFullName_shouldThrowException_whenInvalid(
        String invalidFullName, String expectedMessage) {
      // Arrange
      User user = createDefaultUser();

      // Act & Assert
      assertThatThrownBy(() -> user.updateFullName(invalidFullName))
          .isInstanceOf(InvalidFullNameException.class)
          .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("updatePhone() deve alterar o phone com um valor válido")
    void updatePhone_shouldUpdatePhone_whenValid() {
      // Arrange
      User user = createDefaultUser();
      String newPhone = "+5583999999999";

      // Act
      user.updatePhone(newPhone);

      // Assert
      assertThat(user.getPhone()).isEqualTo(newPhone);
    }

    @ParameterizedTest
    @MethodSource("com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidPhoneProvider")
    @DisplayName("updatePhone() deve lançar exceção para valores inválidos")
    void updatePhone_shouldThrowException_whenInvalid(String invalidPhone, String expectedMessage) {

      // Arrange
      User user = createDefaultUser();

      // Act
      assertThatThrownBy(() -> user.updatePhone(invalidPhone))
          .isInstanceOf(InvalidPhoneException.class)
          .hasMessage(expectedMessage);
    }

    @Test
    @DisplayName("updatePasswordHash() deve alterar o passwordHash com um valor válido")
    void updatePasswordHash_shouldUpdatePasswordHash_whenValid() {
      // Arrange
      User user = createDefaultUser();
      String newPasswordHash = "newHashedPassword";

      // Act
      user.updatePasswordHash(newPasswordHash);

      // Assert
      assertThat(user.getPasswordHash()).isEqualTo(newPasswordHash);
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.domain.model.UserTest#invalidPasswordHashProvider")
    @DisplayName("updatePasswordHash() deve lançar exceção para valores inválidos")
    void updatePasswordHash_shouldThrowException_whenInvalid(
        String invalidPasswordHash, String expectedMessage) {
      // Arrange
      User user = createDefaultUser();

      // Act & Assert
      assertThatThrownBy(() -> user.updatePasswordHash(invalidPasswordHash))
          .isInstanceOf(InvalidPasswordHashException.class)
          .hasMessage(expectedMessage);
    }
  }

  @Nested
  @DisplayName("Testes para Gerenciamento da Conta (status)")
  class AccountManagementTests {

    @Test
    @DisplayName("activateAccount() deve ativar a conta se o status for PENDING_VERIFICATION")
    void activateAccount_shouldActivateAccount_whenPendingVerification() {
      // Arrange
      User user = createDefaultUser(); // 'create' já define o status como PENDING

      // Act
      user.activateAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "LOCKED"})
    @DisplayName(
        "activateAccount() deve lançar exceção quando o status não for PENDING_VERIFICATION")
    void activateAccount_shouldThrowException_whenStatusIsNotPending(AccountStatus invalidStatus) {
      // Arrange
      User user = createUserWithStatus(invalidStatus);

      // Act & Assert
      assertThatThrownBy(user::activateAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está ativada. Você pode fazer login.");
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "LOCKED", "DISABLED"})
    @DisplayName(
        "activateAccount() deve lançar exceção quando o status não for PENDING_VERIFICATION")
    void activateAccount_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = createUserWithStatus(invalidStatus);

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
      User user = createUserWithStatus(status);

      // Act
      user.lockAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    @DisplayName("lockAccount() deve lançar exceção se a conta já estiver bloqueada")
    void lockAccount_shouldThrowException_whenStatusIsLocked() {
      // Arrange
      User user = createUserWithStatus(AccountStatus.LOCKED);

      // Act & Assert
      assertThatThrownBy(user::lockAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está bloqueada.");
    }

    @Test
    @DisplayName("unlockAccount() deve ativar a conta se o status for LOCKED")
    void unlockAccount_shouldActivateAccount_whenStatusIsLocked() {
      // Arrange
      User user = createUserWithStatus(AccountStatus.LOCKED);

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
      User user = createUserWithStatus(invalidStatus);

      // Act & Assert
      assertThatThrownBy(user::unlockAccount)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage("Atenção: A conta já está desbloqueada.");
    }
  }

  @Nested
  @DisplayName("Testes para os Métodos de Verificação de Status (ensure...)")
  class AccountStateGuardTests {
    @Test
    @DisplayName("ensurePendingVerification() não deve lançar exceção quando o status for PENDING")
    void ensurePendingVerification_shouldNotThrowException_whenStatusIsPending() {
      // Arrange
      User user = createUserWithStatus(AccountStatus.PENDING_VERIFICATION);
      // Act & Assert
      assertThatCode(user::ensurePendingVerification).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "LOCKED"})
    @DisplayName("ensurePendingVerification() deve lançar exceção para status inválidos")
    void ensurePendingVerification_shouldThrowException_whenStatusIsNotPending(
        AccountStatus invalidStatus) {
      // Arrange
      User user = createUserWithStatus(invalidStatus);
      // Act & Assert
      assertThatThrownBy(user::ensurePendingVerification)
          .isInstanceOf(AccountStateConflictException.class)
          .hasMessage(
              "Atenção: Só é possível reenviar o código para contas pendentes de verificação.");
    }
  }

  // --- DATA PROVIDERS PARA TESTES PARAMETRIZADOS ---
  private static Stream<Arguments> invalidUsernameProvider() {
    return Stream.of(
        Arguments.of(null, "O nome de usuário não pode ser nulo ou vazio."),
        Arguments.of("", "O nome de usuário não pode ser nulo ou vazio."),
        Arguments.of("  ", "O nome de usuário não pode ser nulo ou vazio."),
        Arguments.of(
            "user with spaces",
            "O nome de usuário deve conter apenas letras, números e underscore (_)."),
        Arguments.of("us", "O nome de usuário deve ter entre 3 e 50 caracteres."),
        Arguments.of("u".repeat(51), "O nome de usuário deve ter entre 3 e 50 caracteres."));
  }

  private static Stream<Arguments> invalidFullNameProvider() {
    return Stream.of(
        Arguments.of("ab", "O nome completo deve ter entre 3 e 100 caracteres."),
        Arguments.of("a".repeat(101), "O nome completo deve ter entre 3 e 100 caracteres."));
  }

  private static Stream<Arguments> invalidPhoneProvider() {
    return Stream.of(
        Arguments.of(null, "O número de telefone não pode ser nulo ou vazio."),
        Arguments.of("", "O número de telefone não pode ser nulo ou vazio."),
        Arguments.of("  ", "O número de telefone não pode ser nulo ou vazio."),
        Arguments.of("123456", "O número de telefone deve estar no formato E.164."),
        Arguments.of("+123 456", "O número de telefone deve estar no formato E.164."),
        Arguments.of("+0123456", "O número de telefone deve estar no formato E.164."));
  }

  private static Stream<Arguments> invalidPasswordHashProvider() {
    return Stream.of(
        Arguments.of(null, "O hash da senha não pode ser nulo ou vazio."),
        Arguments.of("", "O hash da senha não pode ser nulo ou vazio."),
        Arguments.of("  ", "O hash da senha não pode ser nulo ou vazio."));
  }
}

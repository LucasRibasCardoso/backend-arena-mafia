package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatFullNameException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordHashException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUsernameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@DisplayName("Testes unitários para entidade User")
public class UserTest {

  private final String defaultUsername = TestDataProvider.defaultUsername;
  private final String defaultFullName = TestDataProvider.defaultFullName;
  private final String defaultPhone = TestDataProvider.defaultPhone;
  private final String defaultPassword = TestDataProvider.defaultPassword;

  @Nested
  @DisplayName("Testes para os Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("create() deve criar um usuário com valores padrão corretos")
    void create_shouldCreateUserSuccessfully() {
      // Act
      User user = User.create(defaultUsername, defaultFullName, defaultPhone, defaultPassword);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getId()).isNotNull();
      assertThat(user.getUsername()).isEqualTo(defaultUsername);
      assertThat(user.getFullName()).isEqualTo(defaultFullName);
      assertThat(user.getPhone()).isEqualTo(defaultPhone);
      assertThat(user.getPasswordHash()).isEqualTo(defaultPassword);
      assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
      assertThat(user.getCreatedAt()).isBefore(Instant.now());
      assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
    }

    @Nested
    @DisplayName("Cenários de Falha (Validações)")
    class Failure {

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidUsernameProvider")
      @DisplayName("Deve lançar InvalidUsernameFormatException para usernames inválidos")
      void create_shouldThrowException_whenUsernameIsInvalid(
          String invalidUsername, ErrorCode errorCode) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () -> User.create(invalidUsername, defaultFullName, defaultPhone, defaultPassword))
            .isInstanceOf(InvalidUsernameFormatException.class)
            .satisfies(
                ex -> {
                  InvalidUsernameFormatException exception = (InvalidUsernameFormatException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidFullNameProvider")
      @DisplayName("Deve InvalidFormatFullNameException exceção para fullNames inválidos")
      void create_shouldThrowException_whenFullNameIsInvalid(
          String invalidFullName, ErrorCode errorCode) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () -> User.create(defaultUsername, invalidFullName, defaultPhone, defaultPassword))
            .isInstanceOf(InvalidFormatFullNameException.class)
            .satisfies(
                ex -> {
                  InvalidFormatFullNameException exception = (InvalidFormatFullNameException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidPhoneProvider")
      @DisplayName("Deve lançar InvalidFormatPhoneException para telefone inválidos")
      void create_shouldThrowException_whenPhoneIsInvalid(
          String invalidPhone, ErrorCode errorCode) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () -> User.create(defaultUsername, defaultFullName, invalidPhone, defaultPassword))
            .isInstanceOf(InvalidFormatPhoneException.class)
            .satisfies(
                ex -> {
                  InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @NullAndEmptySource
      @DisplayName("create() deve lançar exceção para passwordHashes inválidos")
      void create_shouldThrowException_whenPasswordHashIsInvalid(String invalidPasswordHash) {
        // Arrange, Act & Assert
        assertThatThrownBy(
                () ->
                    User.create(
                        defaultUsername, defaultFullName, defaultPhone, invalidPasswordHash))
            .isInstanceOf(InvalidPasswordHashException.class)
            .satisfies(
                ex -> {
                  InvalidPasswordHashException exception = (InvalidPasswordHashException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_HASH_REQUIRED);
                });
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
      User user = TestDataProvider.createActiveUser();
      String newUsername = "new_valid_user";

      // Act
      user.updateUsername(newUsername);

      // Assert
      assertThat(user.getUsername()).isEqualTo(newUsername);
      assertThat(user.getUpdatedAt()).isAfter(user.getCreatedAt());
      assertThat(user.getUpdatedAt()).isBefore(Instant.now());
    }

    @Test
    @DisplayName("updateFullName() deve alterar o fullName com um valor válido")
    void updateFullName_shouldUpdateFullName_whenValid() {
      // Arrange
      User user = TestDataProvider.createActiveUser();
      String newFullName = "New Valid Name";

      // Act
      user.updateFullName(newFullName);

      // Assert
      assertThat(user.getFullName()).isEqualTo(newFullName);
      assertThat(user.getUpdatedAt()).isAfter(user.getCreatedAt());
      assertThat(user.getUpdatedAt()).isBefore(Instant.now());
    }

    @Test
    @DisplayName("updatePhone() deve alterar o phone com um valor válido")
    void updatePhone_shouldUpdatePhone_whenValid() {
      // Arrange
      User user = TestDataProvider.createActiveUser();
      String newPhone = "+5583999999999";

      // Act
      user.updatePhone(newPhone);

      // Assert
      assertThat(user.getPhone()).isEqualTo(newPhone);
      assertThat(user.getUpdatedAt()).isAfter(user.getCreatedAt());
      assertThat(user.getUpdatedAt()).isBefore(Instant.now());
    }

    @Test
    @DisplayName("updatePasswordHash() deve alterar o passwordHash com um valor válido")
    void updatePasswordHash_shouldUpdatePasswordHash_whenValid() {
      // Arrange
      User user = TestDataProvider.createActiveUser();
      String newPasswordHash = "newHashedPassword";

      // Act
      user.updatePasswordHash(newPasswordHash);

      // Assert
      assertThat(user.getPasswordHash()).isEqualTo(newPasswordHash);
      assertThat(user.getUpdatedAt()).isAfter(user.getCreatedAt());
      assertThat(user.getUpdatedAt()).isBefore(Instant.now());
    }
  }

  @Nested
  @DisplayName("Testes para Gerenciamento da Conta (status)")
  class AccountManagementTests {

    @Test
    @DisplayName("Deve verificar uma conta que está pendente de verificação")
    void confirmVerification_shouldActiveAccount_whenAccountStatusIsPendingVerification() {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().build();
      assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);

      // Act
      user.confirmVerification();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"LOCKED", "DISABLED", "ACTIVE"})
    @DisplayName(
        "confirmVerification() deve lançar AccountStatusForbiddenException para status inválidos")
    void confirmVerification_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();
      assertThat(user.getStatus()).isEqualTo(invalidStatus);

      // Act & Assert
      assertThatThrownBy(user::confirmVerification)
          .isInstanceOf(AccountStatusForbiddenException.class)
          .satisfies(
              ex -> {
                AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.ACCOUNT_NOT_PENDING_VERIFICATION);
              });
    }

    @Test
    @DisplayName("disableAccount() deve desativar uma conta ativa")
    void disableAccount_shouldDisableAccount_whenAccountStatusIsActive() {
      // Arrange
      User user = TestDataProvider.createActiveUser();

      // Act
      user.disableAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.DISABLED);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"LOCKED", "DISABLED", "PENDING_VERIFICATION"})
    @DisplayName(
        "disableAccount() deve lançar AccountStatusForbiddenException para status inválidos")
    void disableAccount_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();
      assertThat(user.getStatus()).isEqualTo(invalidStatus);

      // Act & Assert
      assertThatThrownBy(user::disableAccount)
          .isInstanceOf(AccountStatusForbiddenException.class)
          .satisfies(
              ex -> {
                AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE);
              });
    }

    @Test
    @DisplayName("enableAccount() deve ativar uma conta desativada")
    void enableAccount_shouldEnableAccount_whenAccountStatusIsDisabled() {
      // Arrange
      User user =
          TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.DISABLED).build();

      // Act
      user.enableAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"LOCKED", "ACTIVE", "PENDING_VERIFICATION"})
    @DisplayName(
        "enableAccount() deve lançar AccountStatusForbiddenException para status inválidos")
    void enableAccount_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();

      // Act & Assert
      assertThatThrownBy(user::enableAccount)
          .isInstanceOf(AccountStatusForbiddenException.class)
          .satisfies(
              ex -> {
                AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_DISABLED);
              });
    }

    @Test
    @DisplayName("lockAccount() deve bloquear uma conta ativa")
    void lockAccount_shouldLockAccount_whenAccountStatusIsActive() {
      // Arrange
      User user = TestDataProvider.createActiveUser();

      // Act
      user.lockAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"LOCKED", "DISABLED", "PENDING_VERIFICATION"})
    @DisplayName("lockAccount() deve lançar AccountStatusForbiddenException para status inválidos")
    void lockAccount_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();

      // Act & Assert
      assertThatThrownBy(user::lockAccount)
          .isInstanceOf(AccountStatusForbiddenException.class)
          .satisfies(
              ex -> {
                AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE);
              });
    }

    @Test
    @DisplayName("unlockAccount() deve desbloquear uma conta bloqueada")
    void unlockAccount_shouldUnlockAccount_whenAccountStatusIsLocked() {
      // Arrange
      User user =
          TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.LOCKED).build();

      // Act
      user.unlockAccount();

      // Assert
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "DISABLED", "PENDING_VERIFICATION"})
    @DisplayName(
        "unlockAccount() deve lançar AccountStatusForbiddenException para status inválidos")
    void unlockAccount_shouldThrowException_whenStatusIsInvalid(AccountStatus invalidStatus) {
      // Arrange
      User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();

      // Act & Assert
      assertThatThrownBy(user::unlockAccount)
          .isInstanceOf(AccountStatusForbiddenException.class)
          .satisfies(
              ex -> {
                AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_LOCKED);
              });
    }

    @Nested
    @DisplayName("Testes para os Métodos de Verificação de Status (ensure...)")
    class AccountStateGuardTests {
      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
      @DisplayName(
          "ensureAccountEnabled(): Deve lançar AccountStatusForbiddenException quando a conta não estiver ativa")
      void
          ensureAccountEnabled_shouldThrowAccountStatusForbiddenException_whenAccountStatusIsInvalid(
              AccountStatus invalidStatus, ErrorCode errorCode) {
        // Arrange
        User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();

        // Act & Assert
        assertThatThrownBy(user::ensureAccountEnabled)
            .isInstanceOf(AccountStatusForbiddenException.class)
            .satisfies(
                ex -> {
                  AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidAccountStatusToRequestOtpProvider")
      @DisplayName(
          "ensureCanRequestOtp(): Deve lançar AccountStatusForbiddenException quando a conta não puder solicitar OTP")
      void
          ensureCanRequestOtp_shouldThrowAccountStatusForbiddenException_whenAccountStatusIsInvalid(
              AccountStatus invalidStatus, ErrorCode errorCode) {
        // Arrange
        User user = TestDataProvider.UserBuilder.defaultUser().withStatus(invalidStatus).build();

        // Act & Assert
        assertThatThrownBy(user::ensureCanRequestOtp)
            .isInstanceOf(AccountStatusForbiddenException.class)
            .satisfies(
                ex -> {
                  AccountStatusForbiddenException exception = (AccountStatusForbiddenException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }
    }
  }
}

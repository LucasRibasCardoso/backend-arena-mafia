package com.projetoExtensao.arenaMafia.unit.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.projetoExtensao.arenaMafia.domain.exception.DomainValidationException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.LocalDateTime;
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
      assertNotNull(newUser);
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
  }

  @Nested
  @DisplayName("Falhas ao tentar criar um usuário inválido")
  class FailureScenarios {

    @ParameterizedTest(name = "Deve lançar exceção para username: \"{0}\"")
    @MethodSource("com.projetoExtensao.arenaMafia.unit.model.UserTest#invalidUsernameProvider")
    @DisplayName("Deve lançar DomainValidationException para usernames inválidos")
    void create_shouldThrowExceptionForInvalidUsernames(
        String invalidUsername, String expectMessage) {

      // Arrange & Act & Assert
      assertThatThrownBy(() -> User.create(invalidUsername, fullName, phone, passwordHash))
          .isInstanceOf(DomainValidationException.class)
          .hasMessage(expectMessage);
    }
  }

  @Nested
  @DisplayName("Gerenciamento da conta")
  class AccountManagementTests {
    @Test
    @DisplayName("Deve ativar a conta do usuário")
    void enable_shouldEnableUserAccount() {
      // Arrange
      User user = User.create(username, fullName, phone, passwordHash);

      // Act
      user.activateAccount();

      // Assert
      assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar um erro ao tentar ativar a conta de um usuário já ativo")
    void enable_shouldThrowErrorWhenAccountAlreadyEnabled() {
      // Arrange
      User user = User.create(username, fullName, phone, passwordHash);
      user.activateAccount();

      // Act & Assert
      assertThatThrownBy(user::activateAccount).hasMessage("Atenção: Conta já está ativada.");
    }

    @Test
    @DisplayName("Deve bloquear a conta do usuário")
    void lock_shouldLockUserAccount() {
      // Arrange
      User user = User.create(username, fullName, phone, passwordHash);

      // Act
      user.lockAccount();

      // Assert
      assertThat(user.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("Deve desbloquear a conta do usuário")
    void unlock_shouldUnlockUserAccount() {
      // Arrange
      User user = User.create(username, fullName, phone, passwordHash);
      user.lockAccount();

      // Act
      user.unlockAccount();

      // Assert
      assertThat(user.isAccountNonLocked()).isTrue();
    }
  }

  @Nested
  @DisplayName("Permissões de usuários")
  class UserRoleTest {

    private User createUserWithRole(RoleEnum role) {
      return new User(
          UUID.randomUUID(),
          username,
          fullName,
          phone,
          passwordHash,
          false, // accountNonLocked
          false, // enabled
          role,
          LocalDateTime.now());
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

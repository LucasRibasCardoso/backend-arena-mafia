package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFullNameException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordHashException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUsernameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

  private final UUID id;
  private String username;
  private String fullName;
  private String phone;
  private final RoleEnum role;
  private final Instant createdAt;

  private String passwordHash;
  private AccountStatus status;

  /**
   * Factory Method para criar uma instância de User. Por padrão um usuário será criado com a role
   * ROLE_USER e a conta pendente de verificação
   */
  public static User create(String username, String fullName, String phone, String passwordHash) {
    validateUsername(username);
    validateFullName(fullName);
    validatePhone(phone);
    validatePasswordHash(passwordHash);

    UUID newId = UUID.randomUUID();
    Instant now = Instant.now();
    AccountStatus status = AccountStatus.PENDING_VERIFICATION;

    return new User(
        newId, username, fullName, phone, passwordHash, status, RoleEnum.ROLE_USER, now);
  }

  /**
   * Factory Method para RECONSTRUIR um usuário a partir de dados existentes do banco. Esse metodo é
   * usado pelo MapStruct para mapear uma entidade para User.
   */
  public static User reconstitute(
      UUID id,
      String username,
      String fullName,
      String phone,
      String passwordHash,
      AccountStatus status,
      RoleEnum role,
      Instant createdAt) {

    validateUsername(username);
    validateFullName(fullName);
    validatePhone(phone);
    validatePasswordHash(passwordHash);
    return new User(id, username, fullName, phone, passwordHash, status, role, createdAt);
  }

  private User(
      UUID id,
      String username,
      String fullName,
      String phone,
      String passwordHash,
      AccountStatus status,
      RoleEnum role,
      Instant createdAt) {

    this.id = id;
    this.username = username;
    this.fullName = fullName;
    this.phone = phone;
    this.passwordHash = passwordHash;
    this.status = status;
    this.role = role;
    this.createdAt = createdAt;
  }

  // Validações
  public static void validateUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new InvalidUsernameFormatException("O nome de usuário não pode ser nulo ou vazio.");
    }
    if (username.length() < 3 || username.length() > 50) {
      throw new InvalidUsernameFormatException(
          "O nome de usuário deve ter entre 3 e 50 caracteres.");
    }
    if (!username.matches("^[a-zA-Z0-9_]+$")) {
      throw new InvalidUsernameFormatException(
          "O nome de usuário deve conter apenas letras, números e underscore (_).");
    }
  }

  public static void validateFullName(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      throw new InvalidFullNameException("O nome completo não pode ser nulo ou vazio.");
    }
    if (fullName.length() < 3 || fullName.length() > 100) {
      throw new InvalidFullNameException("O nome completo deve ter entre 3 e 100 caracteres.");
    }
  }

  public static void validatePasswordHash(String passwordHash) {
    if (passwordHash == null || passwordHash.isBlank()) {
      throw new InvalidPasswordHashException("O hash da senha não pode ser nulo ou vazio.");
    }
  }

  public static void validatePhone(String phone) {
    if (phone == null || phone.isBlank()) {
      throw new InvalidPhoneException("O número de telefone não pode ser nulo ou vazio.");
    }
    if (!phone.matches("^\\+[1-9]\\d{1,14}$")) {
      throw new InvalidPhoneException("O número de telefone deve estar no formato E.164.");
    }
  }

  // Atualizar atributos
  public void updatePasswordHash(String newPasswordHash) {
    validatePasswordHash(newPasswordHash);
    this.passwordHash = newPasswordHash;
  }

  public void updateUsername(String newUsername) {
    validateUsername(newUsername);
    this.username = newUsername;
  }

  public void updateFullName(String fullName) {
    validateFullName(fullName);
    this.fullName = fullName;
  }

  public void updatePhone(String newPhone) {
    validatePhone(newPhone);
    this.phone = newPhone;
  }

  // Validar status da conta
  public void ensureAccountEnabled() {
    this.status.validateEnabled();
  }

  public void ensurePendingVerification() {
    if (this.status != AccountStatus.PENDING_VERIFICATION) {
      throw new AccountStateConflictException(
          "Atenção: Só é possível reenviar o código para contas pendentes de verificação.");
    }
  }

  // Gerenciar status da conta
  public void activateAccount() {
    if (!(this.status == AccountStatus.PENDING_VERIFICATION)) {
      throw new AccountStateConflictException(
          "Atenção: A conta já está ativada. Você pode fazer login.");
    }
    this.status = AccountStatus.ACTIVE;
  }

  public void lockAccount() {
    if (this.status == AccountStatus.LOCKED) {
      throw new AccountStateConflictException("Atenção: A conta já está bloqueada.");
    }
    this.status = AccountStatus.LOCKED;
  }

  public void unlockAccount() {
    if (this.status != AccountStatus.LOCKED) {
      throw new AccountStateConflictException("Atenção: A conta já está desbloqueada.");
    }
    this.status = AccountStatus.ACTIVE;
  }

  // Verificar roles
  public boolean isAdmin() {
    return this.role == RoleEnum.ROLE_ADMIN;
  }

  public boolean isManager() {
    return this.role == RoleEnum.ROLE_MANAGER;
  }

  public boolean isUser() {
    return this.role == RoleEnum.ROLE_USER;
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPhone() {
    return phone;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public RoleEnum getRole() {
    return role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public boolean isAccountNonLocked() {
    return this.status != AccountStatus.LOCKED;
  }

  public boolean isEnabled() {
    return this.status == AccountStatus.ACTIVE;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof User user)) return false;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}

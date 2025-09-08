package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatFullNameException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPasswordHashException;
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
  private String passwordHash;
  private AccountStatus status;
  private final RoleEnum role;
  private final Instant createdAt;
  private Instant updatedAt;

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
        newId, username, fullName, phone, passwordHash, status, RoleEnum.ROLE_USER, now, now);
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
      Instant createdAt,
      Instant updatedAt) {
    validateUsername(username);
    validateFullName(fullName);
    validatePhone(phone);
    validatePasswordHash(passwordHash);
    return new User(
        id, username, fullName, phone, passwordHash, status, role, createdAt, updatedAt);
  }

  private User(
      UUID id,
      String username,
      String fullName,
      String phone,
      String passwordHash,
      AccountStatus status,
      RoleEnum role,
      Instant createdAt,
      Instant updatedAt) {

    this.id = id;
    this.username = username;
    this.fullName = fullName;
    this.phone = phone;
    this.passwordHash = passwordHash;
    this.status = status;
    this.role = role;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  // Validações
  public static void validateUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new InvalidUsernameFormatException(ErrorCode.USERNAME_REQUIRED);
    }

    if (username.length() < 3 || username.length() > 50) {
      throw new InvalidUsernameFormatException(ErrorCode.USERNAME_INVALID_LENGTH);
    }

    if (!username.matches("^[a-zA-Z0-9_]+$")) {
      throw new InvalidUsernameFormatException(ErrorCode.USERNAME_INVALID_FORMAT);
    }
  }

  public static void validatePasswordHash(String passwordHash) {
    if (passwordHash == null || passwordHash.isBlank()) {
      throw new InvalidPasswordHashException(ErrorCode.PASSWORD_HASH_REQUIRED);
    }
  }

  public static void validatePhone(String phone) {
    if (phone == null || phone.isBlank()) {
      throw new InvalidFormatPhoneException(ErrorCode.PHONE_REQUIRED);
    }
    if (!phone.matches("^\\+[1-9]\\d{1,14}$")) {
      throw new InvalidFormatPhoneException(ErrorCode.INVALID_FORMAT_PHONE);
    }
  }

  public static void validateFullName(String fullName) {
    if (fullName.isBlank()) {
      throw new InvalidFormatFullNameException(ErrorCode.FULL_NAME_REQUIRED);
    }
    if (fullName.length() < 3 || fullName.length() > 100) {
      throw new InvalidFormatFullNameException(ErrorCode.FULL_NAME_INVALID_LENGTH);
    }
  }

  // Atualizar atributos
  public void updatePasswordHash(String newPasswordHash) {
    validatePasswordHash(newPasswordHash);
    this.passwordHash = newPasswordHash;
    markAsUpdated();
  }

  public void updateUsername(String newUsername) {
    validateUsername(newUsername);
    this.username = newUsername;
    markAsUpdated();
  }

  public void updateFullName(String fullName) {
    if (fullName != null) {
      validateFullName(fullName);
      this.fullName = fullName;
      markAsUpdated();
    }
  }

  public void updatePhone(String newPhone) {
    validatePhone(newPhone);
    this.phone = newPhone;
    markAsUpdated();
  }

  // Validar status da conta
  public void ensureCanRequestOtp() {
    if (this.status == AccountStatus.ACTIVE || this.status == AccountStatus.PENDING_VERIFICATION) {
      return;
    }

    switch (this.status) {
      case LOCKED -> throw new AccountStateConflictException(ErrorCode.ACCOUNT_LOCKED);
      case DISABLED -> throw new AccountStateConflictException(ErrorCode.ACCOUNT_DISABLED);
      default -> throw new AccountStateConflictException(ErrorCode.ACCOUNT_STATE_CONFLICT);
    }
  }

  public void ensureAccountEnabled() {
    this.status.validateEnabled();
  }

  public void ensurePendingVerification() {
    if (this.status != AccountStatus.PENDING_VERIFICATION) {
      throw new AccountStateConflictException(ErrorCode.ACCOUNT_NOT_PENDING_VERIFICATION);
    }
  }

  // Gerenciar status da conta
  public void confirmVerification() {
    if (this.status != AccountStatus.PENDING_VERIFICATION) {
      throw new AccountStateConflictException(ErrorCode.ACCOUNT_NOT_PENDING_VERIFICATION);
    }
    this.status = AccountStatus.ACTIVE;
    markAsUpdated();
  }

  public void disableAccount() {
    if (this.status != AccountStatus.ACTIVE) {
      throw new AccountStateConflictException(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }
    this.status = AccountStatus.DISABLED;
    markAsUpdated();
  }

  public void enableAccount() {
    if (this.status != AccountStatus.DISABLED) {
      throw new AccountStateConflictException(ErrorCode.ACCOUNT_NOT_DISABLED);
    }
    this.status = AccountStatus.ACTIVE;
    markAsUpdated();
  }

  public void lockAccount() {
    if (this.status != AccountStatus.ACTIVE) {
      throw new AccountStateConflictException(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }
    this.status = AccountStatus.LOCKED;
    markAsUpdated();
  }

  public void unlockAccount() {
    if (this.status != AccountStatus.LOCKED) {
      throw new AccountStateConflictException(ErrorCode.ACCOUNT_NOT_LOCKED);
    }
    this.status = AccountStatus.ACTIVE;
    markAsUpdated();
  }

  // Métodos auxiliares
  private void markAsUpdated() {
    this.updatedAt = Instant.now();
  }

  public boolean isEnabled() {
    return this.status == AccountStatus.ACTIVE;
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

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public boolean isAccountNonLocked() {
    return this.status != AccountStatus.LOCKED;
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

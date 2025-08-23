package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.global.DomainValidationException;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.Instant;
import java.util.UUID;

public class User {

  private final UUID id;
  private final String username;
  private final String fullName;
  private final String phone;
  private final String passwordHash;
  private final RoleEnum role;
  private final Instant createdAt;

  private boolean accountNonLocked;
  private boolean enabled;

  /**
   * Factory Method para criar uma instância de User. Por padrão um usuário será criado com a role
   * ROLE_USER, conta desbloqueada e desativada.
   *
   * @param username o nome de usuário (único)
   * @param fullName o nome completo do usuário
   * @param phone o telefone do usuário
   * @param passwordHash o hash da senha do usuário
   * @return uma nova instância de User
   */
  public static User create(String username, String fullName, String phone, String passwordHash) {
    validateUsername(username);

    UUID newId = UUID.randomUUID();
    Instant now = Instant.now();

    return new User(
        newId, username, fullName, phone, passwordHash, true, false, RoleEnum.ROLE_USER, now);
  }

  /**
   * Factory Method para RECONSTRUIR um usuário a partir de dados existentes (do banco). Esse metodo
   * é usado pelo MapStruct para mapear uma entidade para User.
   *
   * @param id o ID do usuário
   * @param username o nome de usuário (único)
   * @param fullName o nome completo do usuário
   * @param phone o telefone do usuário
   * @param passwordHash o hash da senha do usuário
   * @param accountNonLocked se a conta está bloqueada
   * @param enabled se a conta está ativada
   * @param role a role do usuário
   * @param createdAt a data de criação do usuário
   * @return uma instância de User
   */
  public static User reconstitute(
      UUID id,
      String username,
      String fullName,
      String phone,
      String passwordHash,
      boolean accountNonLocked,
      boolean enabled,
      RoleEnum role,
      Instant createdAt) {

    return new User(
        id, username, fullName, phone, passwordHash, accountNonLocked, enabled, role, createdAt);
  }

  private User(
      UUID id,
      String username,
      String fullName,
      String phone,
      String passwordHash,
      boolean accountNonLocked,
      boolean enabled,
      RoleEnum role,
      Instant createdAt) {

    validateUsername(username);
    this.id = id;
    this.username = username;
    this.fullName = fullName;
    this.phone = phone;
    this.passwordHash = passwordHash;
    this.accountNonLocked = accountNonLocked;
    this.enabled = enabled;
    this.role = role;
    this.createdAt = createdAt;
  }

  public static void validateUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new DomainValidationException("O nome de usuário não pode ser nulo ou vazio.");
    }
    if (username.chars().anyMatch(Character::isWhitespace)) {
      throw new DomainValidationException("O nome de usuário não pode conter espaços.");
    }
    if (username.length() < 4 || username.length() > 50) {
      throw new DomainValidationException("O nome de usuário deve ter entre 4 e 50 caracteres.");
    }
  }

  public void activateAccount() {
    if (this.enabled) {
      throw new DomainValidationException("Atenção: Conta já está ativada.");
    }
    this.enabled = true;
  }

  public void lockAccount() {
    this.accountNonLocked = false;
  }

  public void unlockAccount() {
    this.accountNonLocked = true;
  }

  public boolean isAdmin() {
    return this.role == RoleEnum.ROLE_ADMIN;
  }

  public boolean isManager() {
    return this.role == RoleEnum.ROLE_MANAGER;
  }

  public boolean isUser() {
    return this.role == RoleEnum.ROLE_USER;
  }

  // --- Getters ---
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

  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  public boolean isEnabled() {
    return enabled;
  }
}

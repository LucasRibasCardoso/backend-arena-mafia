package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.AccountAlreadyEnabledException;
import com.projetoExtensao.arenaMafia.domain.exception.DomainValidationException;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import java.time.LocalDateTime;
import java.util.UUID;

public class User {

  private final UUID id;
  private final String username;
  private final String fullName;
  private final String phone;
  private final String passwordHash;
  private final RoleEnum role;
  private final LocalDateTime createdAt;

  private boolean accountNonLocked;
  private boolean enabled;

  /**
   * Factory Method para criar usuário. Aplica as regras de negócio para a criação de uma nova
   * conta.
   *
   * @return um objeto User com os valores padrão de um novo registro.
   */
  public static User create(String username, String fullName, String phone, String passwordHash) {
    UUID newId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    // Um novo usuário sempre começa com o papel padrão e desativado.
    return new User(
        newId, username, fullName, phone, passwordHash, true, false, RoleEnum.ROLE_USER, now);
  }

  /**
   * Usado pelo Factory Method acima e pelo Mapper para reconstruir um objeto a partir de dados já
   * existentes (ex: do banco de dados).
   */
  public User(
      UUID id,
      String username,
      String fullName,
      String phone,
      String passwordHash,
      boolean accountNonLocked,
      boolean enabled,
      RoleEnum role,
      LocalDateTime createdAt) {

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

  // --- Métodos de Negócio ---

  // Ativa a conta do usuário.
  public void activate() {
    if (this.enabled) {
      throw new AccountAlreadyEnabledException("Atenção: Conta já está ativada.");
    }
    this.enabled = true;
  }

  // Bloqueia a conta do usuário.
  public void lock() {
    this.accountNonLocked = false;
  }

  // Desbloqueia a conta do usuário.
  public void unlock() {
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

  // --- Validações de Negócio ---
  public void validateUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new DomainValidationException("O nome de usuário não pode ser nulo ou vazio.");
    }
    if (username.length() < 4 || username.length() > 50) {
      throw new DomainValidationException("O nome de usuário deve ter entre 4 e 50 caracteres.");
    }
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  public boolean isEnabled() {
    return enabled;
  }
}

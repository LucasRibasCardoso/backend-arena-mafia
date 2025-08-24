package com.projetoExtensao.arenaMafia.infrastructure.persistence.entity;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_users")
public class UserEntity {

  @Id private UUID id;

  @Column(name = "user_name", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(nullable = false, unique = true, length = 20)
  private String phone;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AccountStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoleEnum role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // Construtor padrão necessário para JPA
  public UserEntity() {}

  // --- Getters e Setters ---
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public void setStatus(AccountStatus status) {
    this.status = status;
  }

  public RoleEnum getRole() {
    return role;
  }

  public void setRole(RoleEnum role) {
    this.role = role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}

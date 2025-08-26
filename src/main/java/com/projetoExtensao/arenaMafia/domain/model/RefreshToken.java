package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RefreshToken {

  private final RefreshTokenVO token;
  private final Instant expiryDate;
  private final User user;
  private final Instant createdAt;

  /**
   * Factory Method para criar uma instância de RefreshToken.
   *
   * @param expirationTimeInDays o tempo para expiração do token em dias
   * @param user o usuário associado a este token
   * @return uma nova instância de RefreshToken
   */
  public static RefreshToken create(Long expirationTimeInDays, User user) {
    RefreshTokenVO token = RefreshTokenVO.generate();
    Instant expiryDate = Instant.now().plus(expirationTimeInDays, ChronoUnit.DAYS);
    Instant createdAt = Instant.now();
    return new RefreshToken(token, expiryDate, user, createdAt);
  }

  /**
   * Factory Method para RECONSTRUIR um token a partir de dados existentes (do banco). Usado pelo
   * Mapper.
   *
   * @param token o token de valor objeto
   * @param expiryDate a data de expiração do token
   * @param user o usuário associado a este token
   * @param createdAt a data de criação do token
   * @return uma nova instância de RefreshToken reconstituída
   */
  public static RefreshToken reconstitute(
      RefreshTokenVO token, Instant expiryDate, User user, Instant createdAt) {
    return new RefreshToken(token, expiryDate, user, createdAt);
  }

  private RefreshToken(RefreshTokenVO token, Instant expiryDate, User user, Instant createdAt) {
    this.token = token;
    this.expiryDate = expiryDate;
    this.user = user;
    this.createdAt = createdAt;
  }

  public void verifyIfNotExpired() {
    if (isExpired()) {
      throw new RefreshTokenExpiredException(
          "Sua sessão expirou. Por favor, faça login novamente.");
    }
  }

  private boolean isExpired() {
    return Instant.now().isAfter(expiryDate);
  }

  // --- Getters ---
  public RefreshTokenVO getToken() {
    return token;
  }

  public Instant getExpiryDate() {
    return expiryDate;
  }

  public User getUser() {
    return user;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

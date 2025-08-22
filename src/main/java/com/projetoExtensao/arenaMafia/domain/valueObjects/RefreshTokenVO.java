package com.projetoExtensao.arenaMafia.domain.valueObjects;

import com.projetoExtensao.arenaMafia.domain.exception.DomainValidationException;
import java.util.UUID;

public record RefreshTokenVO(UUID value) {

  // Construtor compacto para validação
  public RefreshTokenVO {
    if (value == null) {
      throw new DomainValidationException("Refresh token não pode ser nulo.");
    }
  }

  // Factory method para criar um token a partir de uma String
  public static RefreshTokenVO fromString(java.lang.String token) {
    try {
      return new RefreshTokenVO(UUID.fromString(token));
    } catch (IllegalArgumentException e) {
      throw new DomainValidationException("Formato inválido para o refresh token.");
    }
  }

  // Factory method para gerar um novo
  public static RefreshTokenVO generate() {
    return new RefreshTokenVO(UUID.randomUUID());
  }

  @Override
  public java.lang.String toString() {
    return value.toString();
  }
}

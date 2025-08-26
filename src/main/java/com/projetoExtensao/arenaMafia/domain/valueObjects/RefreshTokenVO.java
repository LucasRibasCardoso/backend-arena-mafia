package com.projetoExtensao.arenaMafia.domain.valueObjects;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.RefreshTokenInvalidFormatException;
import java.util.UUID;

public record RefreshTokenVO(UUID value) {

  // Construtor compacto para validação
  public RefreshTokenVO {
    if (value == null) {
      throw new RefreshTokenInvalidFormatException("Refresh token não pode ser nulo.");
    }
  }

  // Factory method para criar um token a partir de uma String
  public static RefreshTokenVO fromString(String token) {
    if (token == null || token.isBlank()) {
      throw new RefreshTokenInvalidFormatException("Refresh token não pode ser nulo ou vazio.");
    }
    try {
      return new RefreshTokenVO(UUID.fromString(token));
    } catch (IllegalArgumentException e) {
      throw new RefreshTokenInvalidFormatException("Formato inválido para o refresh token.");
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

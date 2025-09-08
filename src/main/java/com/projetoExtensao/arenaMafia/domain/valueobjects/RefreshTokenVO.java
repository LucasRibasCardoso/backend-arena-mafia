package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import java.util.UUID;

public record RefreshTokenVO(@JsonValue UUID value) {

  public RefreshTokenVO {
    if (value == null) {
      throw new InvalidTokenFormatException("Refresh token não pode ser nulo.");
    }
  }

  @JsonCreator
  public static RefreshTokenVO fromString(String token) {
    if (token == null || token.isBlank()) {
      throw new InvalidTokenFormatException("Refresh token não pode ser nulo ou vazio.");
    }
    try {
      return new RefreshTokenVO(UUID.fromString(token));
    } catch (IllegalArgumentException e) {
      throw new InvalidTokenFormatException("Formato inválido para o refresh token.");
    }
  }

  public static RefreshTokenVO generate() {
    return new RefreshTokenVO(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return this.value.toString();
  }
}

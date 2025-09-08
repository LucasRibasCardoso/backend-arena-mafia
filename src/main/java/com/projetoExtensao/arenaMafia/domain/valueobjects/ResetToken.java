package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import java.util.UUID;

public record ResetToken(@JsonValue UUID value) {

  public ResetToken {
    if (value == null) {
      throw new InvalidTokenFormatException("O token de redefinição de senha não pode ser nulo.");
    }
  }

  @JsonCreator
  public static ResetToken fromString(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      throw new InvalidTokenFormatException(
          "O token de redefinição de senha não pode ser nulo ou vazio.");
    }
    try {
      return new ResetToken(UUID.fromString(sessionId));
    } catch (IllegalArgumentException e) {
      throw new InvalidTokenFormatException(
          "Formato inválido para o token de redefinição de senha.");
    }
  }

  public static ResetToken generate() {
    return new ResetToken(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return this.value.toString();
  }
}

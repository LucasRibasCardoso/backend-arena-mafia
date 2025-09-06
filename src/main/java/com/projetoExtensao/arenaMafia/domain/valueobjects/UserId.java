package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidUserIdentifierException;
import java.util.UUID;

public record UserId(UUID value) {
  public UserId {
    if (value == null || value.toString().isEmpty()) {
      throw new InvalidUserIdentifierException(
          "Identificador de usuário não pode ser nulo ou vazio.");
    }
  }

  public static UserId fromString(String userIdStr) {
    if (userIdStr == null || userIdStr.isEmpty()) {
      throw new InvalidUserIdentifierException("Identificador de usuário não pode estar vazio.");
    }

    try {
      return new UserId(UUID.fromString(userIdStr.trim()));
    } catch (IllegalArgumentException e) {
      throw new InvalidUserIdentifierException("Identificador de usuário inválido.");
    }
  }

  @Override
  public String toString() {
    return value.toString();
  }
}

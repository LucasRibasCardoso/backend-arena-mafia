package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import java.util.UUID;

public record OtpSessionId(@JsonValue UUID value) {

  public OtpSessionId {
    if (value == null) {
      throw new InvalidTokenFormatException("O ID da sessão OTP não pode ser nulo.");
    }
  }

  @JsonCreator
  public static OtpSessionId fromString(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      throw new InvalidTokenFormatException("O ID da sessão OTP não pode ser nulo ou vazio.");
    }
    try {
      return new OtpSessionId(UUID.fromString(sessionId));
    } catch (IllegalArgumentException e) {
      throw new InvalidTokenFormatException("Formato inválido para o ID da sessão OTP.");
    }
  }

  public static OtpSessionId generate() {
    return new OtpSessionId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return this.value.toString();
  }
}

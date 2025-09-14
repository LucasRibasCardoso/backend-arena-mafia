package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import java.util.UUID;

public record OtpSessionId(@JsonValue UUID value) {

  public OtpSessionId {
    if (value == null) {
      throw new InvalidTokenFormatException(ErrorCode.OTP_SESSION_ID_REQUIRED);
    }
  }

  @JsonCreator
  public static OtpSessionId fromString(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      throw new InvalidTokenFormatException(ErrorCode.OTP_SESSION_ID_REQUIRED);
    }
    try {
      return new OtpSessionId(UUID.fromString(sessionId));
    } catch (IllegalArgumentException e) {
      throw new InvalidTokenFormatException(ErrorCode.OTP_SESSION_ID_INVALID_FORMAT);
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

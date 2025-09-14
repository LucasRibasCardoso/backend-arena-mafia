package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import java.util.UUID;

public record RefreshTokenVO(@JsonValue UUID value) {

  public RefreshTokenVO {
    if (value == null) {
      throw new InvalidTokenFormatException(ErrorCode.REFRESH_TOKEN_REQUIRED);
    }
  }

  @JsonCreator
  public static RefreshTokenVO fromString(String token) {
    if (token == null || token.isBlank()) {
      throw new InvalidTokenFormatException(ErrorCode.REFRESH_TOKEN_REQUIRED);
    }
    try {
      return new RefreshTokenVO(UUID.fromString(token));
    } catch (IllegalArgumentException e) {
      throw new InvalidTokenFormatException(ErrorCode.REFRESH_TOKEN_INVALID_FORMAT);
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

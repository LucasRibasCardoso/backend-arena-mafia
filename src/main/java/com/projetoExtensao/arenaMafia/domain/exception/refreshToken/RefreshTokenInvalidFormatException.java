package com.projetoExtensao.arenaMafia.domain.exception.refreshToken;

public class RefreshTokenInvalidFormatException extends BadRefreshTokenException {
  public RefreshTokenInvalidFormatException(String message) {
    super(message);
  }
}

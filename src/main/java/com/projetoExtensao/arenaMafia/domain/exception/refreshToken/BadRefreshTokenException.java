package com.projetoExtensao.arenaMafia.domain.exception.refreshToken;

public class BadRefreshTokenException extends RuntimeException {
  public BadRefreshTokenException(String message) {
    super(message);
  }
}

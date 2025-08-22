package com.projetoExtensao.arenaMafia.domain.exception;

public class RefreshTokenExpiredException extends RuntimeException {
  public RefreshTokenExpiredException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception;

public class RefreshTokenNotFoundException extends RuntimeException {
  public RefreshTokenNotFoundException(String message) {
    super(message);
  }
}

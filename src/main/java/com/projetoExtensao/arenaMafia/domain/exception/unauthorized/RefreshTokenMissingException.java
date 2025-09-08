package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

public class RefreshTokenMissingException extends UnauthorizedException {
  public RefreshTokenMissingException(String message) {
    super(message);
  }
}

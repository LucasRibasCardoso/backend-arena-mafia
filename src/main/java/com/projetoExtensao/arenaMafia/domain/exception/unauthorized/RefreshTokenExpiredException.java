package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

public class RefreshTokenExpiredException extends UnauthorizedException {
  public RefreshTokenExpiredException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

public class RefreshTokenNotFoundException extends UnauthorizedException {
  public RefreshTokenNotFoundException(String message) {
    super(message);
  }
}

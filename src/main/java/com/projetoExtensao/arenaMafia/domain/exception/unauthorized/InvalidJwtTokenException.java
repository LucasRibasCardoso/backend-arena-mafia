package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

public class InvalidJwtTokenException extends UnauthorizedException {
  public InvalidJwtTokenException(String message) {
    super(message);
  }
}

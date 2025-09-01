package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class RefreshTokenInvalidFormatException extends BadRequestException {
  public RefreshTokenInvalidFormatException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidTokenFormatException extends BadRequestException {
  public InvalidTokenFormatException(String message) {
    super(message);
  }
}

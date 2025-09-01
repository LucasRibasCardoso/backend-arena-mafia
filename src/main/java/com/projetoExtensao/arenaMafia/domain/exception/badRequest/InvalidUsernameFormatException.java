package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidUsernameFormatException extends BadRequestException {
  public InvalidUsernameFormatException(String message) {
    super(message);
  }
}

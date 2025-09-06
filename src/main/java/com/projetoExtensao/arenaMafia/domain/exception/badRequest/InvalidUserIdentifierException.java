package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidUserIdentifierException extends BadRequestException {
  public InvalidUserIdentifierException(String message) {
    super(message);
  }
}

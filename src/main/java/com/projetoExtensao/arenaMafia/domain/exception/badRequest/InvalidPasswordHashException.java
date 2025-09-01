package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidPasswordHashException extends BadRequestException {
  public InvalidPasswordHashException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class IncorrectPasswordException extends BadRequestException {
  public IncorrectPasswordException(String message) {
    super(message);
  }
}

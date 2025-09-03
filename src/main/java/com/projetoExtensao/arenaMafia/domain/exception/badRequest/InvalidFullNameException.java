package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidFullNameException extends BadRequestException {
  public InvalidFullNameException(String message) {
    super(message);
  }
}

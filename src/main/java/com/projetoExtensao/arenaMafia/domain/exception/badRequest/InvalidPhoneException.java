package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidPhoneException extends BadRequestException {
  public InvalidPhoneException(String message) {
    super(message);
  }
}

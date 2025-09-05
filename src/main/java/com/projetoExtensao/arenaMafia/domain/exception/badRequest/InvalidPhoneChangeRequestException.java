package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidPhoneChangeRequestException extends BadRequestException {
  public InvalidPhoneChangeRequestException(String message) {
    super(message);
  }
}

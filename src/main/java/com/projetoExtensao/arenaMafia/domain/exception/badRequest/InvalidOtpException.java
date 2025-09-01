package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidOtpException extends BadRequestException {
  public InvalidOtpException(String message) {
    super(message);
  }
}

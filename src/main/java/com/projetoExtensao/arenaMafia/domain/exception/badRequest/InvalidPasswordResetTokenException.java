package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

public class InvalidPasswordResetTokenException extends BadRequestException {
  public InvalidPasswordResetTokenException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPasswordResetTokenException extends BadRequestException {
  public InvalidPasswordResetTokenException() {
    super(ErrorCode.RESET_TOKEN_INVALID_OR_EXPIRED);
  }
}

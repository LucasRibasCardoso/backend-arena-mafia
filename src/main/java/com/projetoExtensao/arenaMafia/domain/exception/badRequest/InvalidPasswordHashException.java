package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidPasswordHashException extends BadRequestException {
  public InvalidPasswordHashException(ErrorCode errorCode) {
    super(errorCode);
  }
}

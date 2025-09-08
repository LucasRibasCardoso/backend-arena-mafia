package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidUsernameFormatException extends BadRequestException {
  public InvalidUsernameFormatException(ErrorCode errorCode) {
    super(errorCode);
  }
}

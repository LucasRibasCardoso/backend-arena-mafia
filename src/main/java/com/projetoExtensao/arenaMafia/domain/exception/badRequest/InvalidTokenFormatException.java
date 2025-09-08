package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidTokenFormatException extends BadRequestException {
  public InvalidTokenFormatException(ErrorCode errorCode) {
    super(errorCode);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidFormatFullNameException extends BadRequestException {
  public InvalidFormatFullNameException(ErrorCode errorCode) {
    super(errorCode);
  }
}

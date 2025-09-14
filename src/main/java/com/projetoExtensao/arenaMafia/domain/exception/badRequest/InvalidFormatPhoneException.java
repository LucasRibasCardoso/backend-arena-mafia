package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidFormatPhoneException extends BadRequestException {

  public InvalidFormatPhoneException(ErrorCode errorCode) {
    super(errorCode);
  }
}

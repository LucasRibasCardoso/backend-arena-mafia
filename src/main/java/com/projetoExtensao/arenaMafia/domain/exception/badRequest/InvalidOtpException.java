package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidOtpException extends BadRequestException {
  public InvalidOtpException(ErrorCode errorCode) {
    super(errorCode);
  }
}

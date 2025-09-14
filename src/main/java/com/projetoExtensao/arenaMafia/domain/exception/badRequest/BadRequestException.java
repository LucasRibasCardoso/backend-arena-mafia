package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class BadRequestException extends ApplicationException {
  public BadRequestException(ErrorCode errorCode) {
    super(errorCode);
  }
}

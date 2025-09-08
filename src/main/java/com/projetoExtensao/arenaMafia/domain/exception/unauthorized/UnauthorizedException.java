package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class UnauthorizedException extends ApplicationException {
  public UnauthorizedException(ErrorCode errorCode) {
    super(errorCode);
  }
}

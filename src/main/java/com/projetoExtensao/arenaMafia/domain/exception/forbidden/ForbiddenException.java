package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ForbiddenException extends ApplicationException {
  public ForbiddenException(ErrorCode errorCode) {
    super(errorCode);
  }
}

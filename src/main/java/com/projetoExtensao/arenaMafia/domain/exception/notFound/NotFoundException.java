package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class NotFoundException extends ApplicationException {
  public NotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}

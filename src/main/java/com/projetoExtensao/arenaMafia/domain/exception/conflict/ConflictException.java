package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ConflictException extends ApplicationException {
  public ConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}

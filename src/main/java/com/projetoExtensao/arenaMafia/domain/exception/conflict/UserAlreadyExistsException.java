package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class UserAlreadyExistsException extends ConflictException {
  public UserAlreadyExistsException(ErrorCode errorCode) {
    super(errorCode);
  }
}

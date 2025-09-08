package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AccountStateConflictException extends ConflictException {
  public AccountStateConflictException(ErrorCode errorCode) {
    super(errorCode);
  }
}

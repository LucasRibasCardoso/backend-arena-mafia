package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AccountStatusForbiddenException extends ForbiddenException {
  public AccountStatusForbiddenException(ErrorCode errorCode) {
    super(errorCode);
  }
}

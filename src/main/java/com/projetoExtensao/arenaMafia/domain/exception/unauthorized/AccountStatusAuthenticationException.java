package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class AccountStatusAuthenticationException extends UnauthorizedException {

  public AccountStatusAuthenticationException(ErrorCode errorCode) {
    super(errorCode);
  }
}

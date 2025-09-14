package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidCredentialsException extends UnauthorizedException {
  public InvalidCredentialsException() {
    super(ErrorCode.INVALID_CREDENTIALS);
  }
}

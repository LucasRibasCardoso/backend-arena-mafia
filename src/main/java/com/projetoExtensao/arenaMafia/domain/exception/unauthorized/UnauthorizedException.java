package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;

public class UnauthorizedException extends AuthenticationException {
  private final ErrorCode errorCode;

  public UnauthorizedException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}

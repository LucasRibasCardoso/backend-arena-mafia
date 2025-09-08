package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidJwtTokenException extends UnauthorizedException {
  public InvalidJwtTokenException() {
    super(ErrorCode.JWT_TOKEN_INVALID_OR_EXPIRED);
  }
}

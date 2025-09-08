package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidJwtTokenException extends UnauthorizedException {
  public InvalidJwtTokenException() {
    super(ErrorCode.INVALID_OR_EXPIRED_JWT_TOKEN);
  }
}

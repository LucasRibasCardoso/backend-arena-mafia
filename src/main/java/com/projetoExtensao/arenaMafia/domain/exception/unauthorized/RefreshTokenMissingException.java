package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class RefreshTokenMissingException extends UnauthorizedException {
  public RefreshTokenMissingException() {
    super(ErrorCode.REFRESH_TOKEN_MISSING);
  }
}

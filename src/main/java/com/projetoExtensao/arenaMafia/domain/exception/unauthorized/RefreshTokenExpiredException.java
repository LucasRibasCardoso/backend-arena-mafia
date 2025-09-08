package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class RefreshTokenExpiredException extends UnauthorizedException {
  public RefreshTokenExpiredException() {
    super(ErrorCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);
  }
}

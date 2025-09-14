package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class RefreshTokenNotFoundException extends UnauthorizedException {
  public RefreshTokenNotFoundException() {
    super(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
  }
}

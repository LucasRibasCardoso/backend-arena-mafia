package com.projetoExtensao.arenaMafia.domain.exception.refreshToken;

public class RefreshTokenInvalidFormat extends BadRefreshTokenException {
  public RefreshTokenInvalidFormat(String message) {
    super(message);
  }
}

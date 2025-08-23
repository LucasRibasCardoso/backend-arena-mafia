package com.projetoExtensao.arenaMafia.domain.exception.refreshToken;

public class RefreshTokenExpiredException extends BadRefreshTokenException {
  public RefreshTokenExpiredException(String message) {
    super(message);
  }
}

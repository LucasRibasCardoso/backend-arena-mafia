package com.projetoExtensao.arenaMafia.domain.exception.refreshToken;


public class RefreshTokenNotFoundException extends BadRefreshTokenException {
  public RefreshTokenNotFoundException(String message) {
    super(message);
  }
}

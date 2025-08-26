package com.projetoExtensao.arenaMafia.domain.exception.unauthorized;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;

public class UnauthorizedException extends ApplicationException {
  public UnauthorizedException(String message) {
    super(message);
  }
}

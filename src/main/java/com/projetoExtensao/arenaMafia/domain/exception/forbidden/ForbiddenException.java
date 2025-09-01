package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;

public class ForbiddenException extends ApplicationException {
  public ForbiddenException(String message) {
    super(message);
  }
}

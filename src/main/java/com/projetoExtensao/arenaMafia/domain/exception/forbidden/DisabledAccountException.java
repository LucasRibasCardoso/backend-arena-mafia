package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

public class DisabledAccountException extends ForbiddenException {
  public DisabledAccountException(String message) {
    super(message);
  }
}

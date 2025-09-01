package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

public class AccountNotVerifiedException extends ForbiddenException {
  public AccountNotVerifiedException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.forbidden;

public class AccountLockedException extends ForbiddenException {
  public AccountLockedException(String message) {
    super(message);
  }
}

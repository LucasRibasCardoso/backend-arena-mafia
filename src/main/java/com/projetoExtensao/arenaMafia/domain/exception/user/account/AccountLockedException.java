package com.projetoExtensao.arenaMafia.domain.exception.user.account;


public class AccountLockedException extends AccountException {
  public AccountLockedException(String message) {
    super(message);
  }
}

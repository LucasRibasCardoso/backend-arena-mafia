package com.projetoExtensao.arenaMafia.domain.exception.user.account;

public class DisabledAccountException extends AccountException {
  public DisabledAccountException(String message) {
    super(message);
  }
}

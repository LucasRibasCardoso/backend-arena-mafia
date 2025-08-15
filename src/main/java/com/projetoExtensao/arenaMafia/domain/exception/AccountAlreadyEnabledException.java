package com.projetoExtensao.arenaMafia.domain.exception;

public class AccountAlreadyEnabledException extends RuntimeException {
  public AccountAlreadyEnabledException(String message) {
    super(message);
  }
}

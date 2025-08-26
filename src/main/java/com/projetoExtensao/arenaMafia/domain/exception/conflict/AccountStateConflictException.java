package com.projetoExtensao.arenaMafia.domain.exception.conflict;

public class AccountStateConflictException extends ConflictException {
  public AccountStateConflictException(String message) {
    super(message);
  }
}

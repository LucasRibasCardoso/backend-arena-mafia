package com.projetoExtensao.arenaMafia.domain.exception.conflict;

public class UserAlreadyExistsException extends ConflictException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}

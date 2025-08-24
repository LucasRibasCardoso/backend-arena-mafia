package com.projetoExtensao.arenaMafia.domain.exception.user;

public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.user;

public class PasswordDoNotCoincidException extends RuntimeException {
  public PasswordDoNotCoincidException(String message) {
    super(message);
  }
}

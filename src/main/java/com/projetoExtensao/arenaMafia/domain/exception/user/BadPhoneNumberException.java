package com.projetoExtensao.arenaMafia.domain.exception.user;

public class BadPhoneNumberException extends RuntimeException {
  public BadPhoneNumberException() {
    super();
  }

  public BadPhoneNumberException(String message) {
    super(message);
  }
}

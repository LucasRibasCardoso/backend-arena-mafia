package com.projetoExtensao.arenaMafia.domain.exception.notFound;

public class UserNotFoundException extends NotFoundException {
  public UserNotFoundException(String message) {
    super(message);
  }
}

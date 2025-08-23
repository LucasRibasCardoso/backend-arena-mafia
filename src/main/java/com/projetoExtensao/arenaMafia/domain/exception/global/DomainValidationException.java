package com.projetoExtensao.arenaMafia.domain.exception.global;

public class DomainValidationException extends RuntimeException {
  public DomainValidationException(String message) {
    super(message);
  }
}

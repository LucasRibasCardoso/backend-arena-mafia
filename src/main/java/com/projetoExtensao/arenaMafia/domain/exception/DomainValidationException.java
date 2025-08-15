package com.projetoExtensao.arenaMafia.domain.exception;

public class DomainValidationException extends RuntimeException {
  public DomainValidationException(String message) {
    super(message);
  }
}

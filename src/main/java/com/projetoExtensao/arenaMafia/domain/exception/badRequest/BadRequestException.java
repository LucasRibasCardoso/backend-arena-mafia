package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;

public class BadRequestException extends ApplicationException {
  public BadRequestException(String message) {
    super(message);
  }
}

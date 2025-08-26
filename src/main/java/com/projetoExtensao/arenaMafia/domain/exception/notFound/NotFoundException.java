package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;

public class NotFoundException extends ApplicationException {
  public NotFoundException(String message) {
    super(message);
  }
}

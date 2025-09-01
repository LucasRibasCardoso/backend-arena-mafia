package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;

public class ConflictException extends ApplicationException {
  public ConflictException(String message) {
    super(message);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class IncorrectCurrentPasswordException extends BadRequestException {
  public IncorrectCurrentPasswordException() {
    super(ErrorCode.PASSWORD_CURRENT_INCORRECT);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class UserNotFoundException extends NotFoundException {
  public UserNotFoundException() {
    super(ErrorCode.USER_NOT_FOUND);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PhoneChangeNotFoundException extends NotFoundException {
  public PhoneChangeNotFoundException() {
    super(ErrorCode.PHONE_CHANGE_NOT_INITIATED);
  }
}

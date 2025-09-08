package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PhoneChangeNotInitiatedException extends NotFoundException {
  public PhoneChangeNotInitiatedException() {
    super(ErrorCode.PHONE_CHANGE_NOT_INITIATED);
  }
}

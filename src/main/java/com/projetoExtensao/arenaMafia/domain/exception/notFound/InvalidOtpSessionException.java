package com.projetoExtensao.arenaMafia.domain.exception.notFound;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidOtpSessionException extends NotFoundException {
  public InvalidOtpSessionException() {
    super(ErrorCode.OTP_SESSION_INVALID_OR_EXPIRED);
  }
}

package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class InvalidOtpSessionException extends BadRequestException {
  public InvalidOtpSessionException() {
    super(ErrorCode.OTP_SESSION_ID_INCORRECT_OR_EXPIRED);
  }
}

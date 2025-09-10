package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import java.security.SecureRandom;

public record OtpCode(@JsonValue String value) {

  private static final SecureRandom random = new SecureRandom();

  public OtpCode {
    if (value == null || value.isBlank()) {
      throw new InvalidOtpException(ErrorCode.OTP_CODE_REQUIRED);
    }
    if (!value.matches("\\d{6}")) {
      throw new InvalidOtpException(ErrorCode.OTP_CODE_INVALID_FORMAT);
    }
  }

  @JsonCreator
  public static OtpCode fromString(String value) {
    return new OtpCode(value);
  }

  public static OtpCode generate() {
    int number = random.nextInt(900000) + 100000;
    return new OtpCode(String.valueOf(number));
  }

  @Override
  public String toString() {
    return this.value;
  }
}

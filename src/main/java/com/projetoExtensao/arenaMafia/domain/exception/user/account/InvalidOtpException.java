package com.projetoExtensao.arenaMafia.domain.exception.user.account;


public class InvalidOtpException extends RuntimeException {
  public InvalidOtpException(String message) {
    super(message);
  }
}

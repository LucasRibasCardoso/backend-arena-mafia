package com.projetoExtensao.arenaMafia.domain.exception;

public class ApplicationException extends RuntimeException {

  private final ErrorCode errorCode;

  public ApplicationException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}

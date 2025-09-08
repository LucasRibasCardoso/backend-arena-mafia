package com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
    Instant timestamp,
    int status,
    String errorCode,
    String developerMessage,
    String path,
    List<FieldErrorResponseDto> fieldErrors) {

  public static ErrorResponseDto forGeneralError(int status, ErrorCode errorCode, String path) {
    Instant timestamp = Instant.now();
    return new ErrorResponseDto(
        timestamp, status, errorCode.name(), errorCode.getDefaultMessage(), path, null);
  }

  public static ErrorResponseDto forValidationErrors(
      String path, List<FieldErrorResponseDto> fieldErrors) {

    ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
    return new ErrorResponseDto(
        Instant.now(),
        HttpStatus.BAD_REQUEST.value(),
        errorCode.name(),
        errorCode.getDefaultMessage(),
        path,
        fieldErrors);
  }
}

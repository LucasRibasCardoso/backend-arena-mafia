package com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * DTO padrão para respostas de erro da API. A anotação @JsonInclude(JsonInclude.Include.NON_NULL)
 * garante que campos nulos (como a lista de erros de campo) não sejam incluídos no JSON final.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
    Instant timestamp,
    int status,
    ErrorCode errorCode,
    String message,
    String path,
    List<FieldErrorResponseDto> fieldErrors) {
  // Metodo de fábrica para erros gerais (sem detalhes de campos)
  public static ErrorResponseDto forGeneralError(
      int status, ErrorCode errorCode, String message, String path) {
    return new ErrorResponseDto(Instant.now(), status, errorCode, message, path, null);
  }

  // Metodo de fábrica para erros de validação (com detalhes de campos)
  public static ErrorResponseDto forValidationErrors(
      ErrorCode errorCode, String message, String path, List<FieldErrorResponseDto> fieldErrors) {
    return new ErrorResponseDto(
        Instant.now(), HttpStatus.BAD_REQUEST.value(), errorCode, message, path, fieldErrors);
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    String message,
    String path,
    List<FieldErrorResponseDto> fieldErrors) {
  // Metodo de fábrica para erros gerais (sem detalhes de campos)
  public static ErrorResponseDto forGeneralError(int status, String message, String path) {
    return new ErrorResponseDto(Instant.now(), status, message, path, null);
  }

  // Metodo de fábrica para erros de validação (com detalhes de campos)
  public static ErrorResponseDto forValidationErrors(
      String message, String path, List<FieldErrorResponseDto> fieldErrors) {
    return new ErrorResponseDto(
        Instant.now(), HttpStatus.BAD_REQUEST.value(), message, path, fieldErrors);
  }
}

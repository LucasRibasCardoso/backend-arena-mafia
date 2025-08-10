package com.projetoExtensao.arenaMafia.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * DTO padrão para respostas de erro da API. A anotação @JsonInclude(JsonInclude.Include.NON_NULL)
 * garante que campos nulos (como a lista de erros de campo) não sejam incluídos no JSON final.
 *
 * @param timestamp O momento em que o erro ocorreu.
 * @param status O código de status HTTP.
 * @param message Uma mensagem de erro geral.
 * @param path O caminho da URI onde o erro ocorreu.
 * @param fieldErrors Uma lista detalhando erros de validação de campos específicos (opcional).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String message,
    String path,
    List<FieldErrorResponse> fieldErrors) {

  // Metodo de fábrica para erros gerais (sem detalhes de campos)
  public static ErrorResponse forGeneralError(int status, String message, String path) {
    return new ErrorResponse(Instant.now(), status, message, path, null);
  }

  // Metodo de fábrica para erros de validação (com detalhes de campos)
  public static ErrorResponse forValidationErrors(
      String message, String path, List<FieldErrorResponse> fieldErrors) {
    return new ErrorResponse(
        Instant.now(), HttpStatus.BAD_REQUEST.value(), message, path, fieldErrors);
  }
}

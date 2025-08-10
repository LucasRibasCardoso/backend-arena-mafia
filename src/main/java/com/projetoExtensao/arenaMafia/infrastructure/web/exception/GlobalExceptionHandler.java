package com.projetoExtensao.arenaMafia.infrastructure.web.exception;

import com.projetoExtensao.arenaMafia.infrastructure.web.dto.response.ErrorResponse;
import com.projetoExtensao.arenaMafia.infrastructure.web.dto.response.FieldErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handlerException(HttpServletRequest request) {
    ErrorResponse errorResponse =
        ErrorResponse.forGeneralError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Ocorreu um erro inesperado! Por favor, tente novamente mais tarde.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {

    // Obtém os erros de validação dos campos
    List<FieldErrorResponse> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();

    ErrorResponse errorResponse =
        ErrorResponse.forValidationErrors(
            "Erro de validação nos campos informados.", request.getRequestURI(), fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      HttpServletRequest request) {

    ErrorResponse errorResponse =
        ErrorResponse.forGeneralError(
            HttpStatus.BAD_REQUEST.value(),
            "Erro de integridade de dados. Verifique os dados informados.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }
}

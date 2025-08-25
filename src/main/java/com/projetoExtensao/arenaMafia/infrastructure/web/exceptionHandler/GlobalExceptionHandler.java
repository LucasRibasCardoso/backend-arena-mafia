package com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.global.DomainValidationException;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.FieldErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order()
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handlerException(HttpServletRequest request) {
    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Erro interno do servidor. Por favor, tente novamente mais tarde.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponseDto);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {

    Map<String, String> errorMap = new LinkedHashMap<>();

    // Itera sobre todos os erros (de campo e globais)
    e.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String key;
              if (error instanceof FieldError fieldError) {
                key = fieldError.getField();
              } else {
                key = error.getObjectName();
              }
              errorMap.putIfAbsent(key, error.getDefaultMessage());
            });

    // Converte o mapa para a lista de DTOs de erro
    List<FieldErrorResponseDto> fieldErrors =
        errorMap.entrySet().stream()
            .map(entry -> new FieldErrorResponseDto(entry.getKey(), entry.getValue()))
            .toList();

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forValidationErrors(
            "Erro de validação. Verifique os campos informados.", request.getRequestURI(), fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
      HttpServletRequest request) {

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.CONFLICT.value(),
            "Erro de integridade de dados. Verifique os dados informados.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponseDto);
  }

  @ExceptionHandler(DomainValidationException.class)
  public ResponseEntity<ErrorResponseDto> handleDomainValidationException(
      DomainValidationException e, HttpServletRequest request) {

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.BAD_REQUEST.value(), e.getMessage(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.web.exception;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadRequestException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.ForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.NotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.UnauthorizedException;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // ===============================================================================================
  // HANDLERS PARA A HIERARQUIA DE EXCEÇÕES DE NEGÓCIO
  // ===============================================================================================

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleNotFoundException(
      NotFoundException e, HttpServletRequest request) {
    return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponseDto> handleConflictException(
      ConflictException e, HttpServletRequest request) {
    return createErrorResponse(HttpStatus.CONFLICT, e.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponseDto> handleForbiddenException(
      ForbiddenException e, HttpServletRequest request) {
    return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponseDto> handleUnauthorizedException(
      UnauthorizedException e, HttpServletRequest request) {
    return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponseDto> handleBadRequestException(
      BadRequestException e, HttpServletRequest request) {
    return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request.getRequestURI());
  }

  // ===============================================================================================
  // HANDLERS PARA EXCEÇÕES ESPECÍFICAS DO SPRING E VALIDAÇÃO
  // ===============================================================================================
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
            "Erro de validação. Verifique os campos informados.",
            request.getRequestURI(),
            fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponseDto);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {

    String detailedMessage =
        "O valor '" + e.getValue() + "' é inválido para o parâmetro '" + e.getName() + "'.";

    Throwable rootCause = NestedExceptionUtils.getRootCause(e);
    if (rootCause instanceof BadRequestException) {
      detailedMessage = rootCause.getMessage();
    }
    return createErrorResponse(HttpStatus.BAD_REQUEST, detailedMessage, request.getRequestURI());
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(
      HttpServletRequest request) {
    return createErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "Credenciais inválidas. Por favor, verifique seu usuário e senha.",
        request.getRequestURI());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(HttpServletRequest request) {
    return createErrorResponse(
        HttpStatus.FORBIDDEN,
        "Acesso negado. Você não tem permissão para acessar este recurso.",
        request.getRequestURI());
  }

  // ===============================================================================================
  // HANDLERS GENÉRICOS
  // ===============================================================================================
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
      HttpServletRequest request) {
    return createErrorResponse(
        HttpStatus.CONFLICT,
        "Conflito de dados. O recurso que você está tentando criar ou atualizar já existe.",
        request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleGenericException(
      Exception e, HttpServletRequest request) {

    logger.error("Ocorreu um erro inesperado", e);
    return createErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Erro interno do servidor. Por favor, tente novamente mais tarde.",
        request.getRequestURI());
  }

  private ResponseEntity<ErrorResponseDto> createErrorResponse(
      HttpStatus status, String message, String path) {

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(status.value(), message, path);

    return ResponseEntity.status(status).body(errorResponseDto);
  }
}

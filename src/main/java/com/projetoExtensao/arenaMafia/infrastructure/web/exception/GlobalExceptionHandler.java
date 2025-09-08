package com.projetoExtensao.arenaMafia.infrastructure.web.exception;

import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponseDto> handleApplicationException(
      ApplicationException e, HttpServletRequest request) {
    return buildErrorResponseEntity(mapExceptionToStatus(e), e.getErrorCode(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {

    List<FieldErrorResponseDto> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError -> {
                  String errorCodeString = fieldError.getDefaultMessage();
                  String devMessage;
                  try {
                    ErrorCode errorCode = ErrorCode.valueOf(errorCodeString);
                    devMessage = errorCode.getDefaultMessage();
                  } catch (IllegalArgumentException ex) {
                    devMessage = "Código de erro de validação não mapeado: " + errorCodeString;
                  }
                  return new FieldErrorResponseDto(
                      fieldError.getField(), errorCodeString, devMessage);
                })
            .toList();

    return buildErrorResponseEntity(fieldErrors, request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {

    Throwable rootCause = NestedExceptionUtils.getRootCause(e);
    if (rootCause instanceof ApplicationException appException) {
      return handleApplicationException(appException, request);
    }
    return buildErrorResponseEntity(
        HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST_PARAMETER, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(HttpServletRequest request) {
    return buildErrorResponseEntity(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
      HttpServletRequest request) {
    return buildErrorResponseEntity(
        HttpStatus.CONFLICT, ErrorCode.DATA_INTEGRITY_VIOLATION, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleGenericException(
      Exception e, HttpServletRequest request) {

    logger.error("Ocorreu um erro inesperado: ", e);
    return buildErrorResponseEntity(
        HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNEXPECTED_ERROR, request);
  }

  private HttpStatus mapExceptionToStatus(ApplicationException e) {
    if (e instanceof NotFoundException) return HttpStatus.NOT_FOUND;
    if (e instanceof ConflictException) return HttpStatus.CONFLICT;
    if (e instanceof UnauthorizedException) return HttpStatus.UNAUTHORIZED;
    if (e instanceof ForbiddenException) return HttpStatus.FORBIDDEN;
    return HttpStatus.BAD_REQUEST;
  }

  private ResponseEntity<ErrorResponseDto> buildErrorResponseEntity(
      HttpStatus status, ErrorCode errorCode, HttpServletRequest request) {
    ErrorResponseDto responseBody =
        ErrorResponseDto.forGeneralError(status.value(), errorCode, request.getRequestURI());
    return ResponseEntity.status(status).body(responseBody);
  }

  private ResponseEntity<ErrorResponseDto> buildErrorResponseEntity(
      List<FieldErrorResponseDto> fieldErrors, HttpServletRequest request) {
    ErrorResponseDto responseBody =
        ErrorResponseDto.forValidationErrors(request.getRequestURI(), fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
  }
}

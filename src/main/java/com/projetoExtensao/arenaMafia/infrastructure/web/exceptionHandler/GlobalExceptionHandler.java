package com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.DomainValidationException;
import com.projetoExtensao.arenaMafia.domain.exception.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.RefreshTokenNotFoundException;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.FieldErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
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

    List<FieldErrorResponseDto> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField, // Key: field name
                    FieldError::getDefaultMessage, // Value: error message
                    (existingValue, newValue) -> existingValue) // mantém o valor existente
                )
            .entrySet() // Converte o Map em um SET de FieldErrorResponseDto
            .stream()
            .map(entry -> new FieldErrorResponseDto(entry.getKey(), entry.getValue()))
            .toList(); // Converte o SET de Map.Entry em uma lista de FieldErrorResponseDto

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forValidationErrors(
            "Erro de validação nos campos informados.", request.getRequestURI(), fieldErrors);

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

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(HttpServletRequest request) {

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.FORBIDDEN.value(),
            "Acesso negado. Você não tem permissão para acessar este recurso.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponseDto);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(
      HttpServletRequest request) {

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.UNAUTHORIZED.value(),
            "Credenciais inválidas. Por favor, verifique seu usuário e senha.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
  }

  @ExceptionHandler(RefreshTokenExpiredException.class)
  public ResponseEntity<ErrorResponseDto> handleRefreshTokenExpiredException(
      RefreshTokenExpiredException e, HttpServletRequest request) {
    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.UNAUTHORIZED.value(), e.getMessage(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
  }

  @ExceptionHandler(RefreshTokenNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleRefreshTokenNotFoundException(
      RefreshTokenNotFoundException e, HttpServletRequest request) {
    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.NOT_FOUND.value(), e.getMessage(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponseDto);
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.BadRefreshTokenException;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.AccountException;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(HttpServletRequest request) {

    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.FORBIDDEN.value(),
            "Acesso negado. Você não tem permissão para acessar este recurso.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponseDto);
  }

  @ExceptionHandler(BadRefreshTokenException.class)
  public ResponseEntity<ErrorResponseDto> handleBadRefreshTokenException(
      HttpServletRequest request) {

    String message = "Token de atualização inválido ou expirado. Por favor, faça login novamente.";
    ErrorResponseDto errorResponseDto =
        ErrorResponseDto.forGeneralError(
            HttpStatus.UNAUTHORIZED.value(), message, request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponseDto> handlerBadCredentialsException(
      HttpServletRequest request) {

    ErrorResponseDto response =
        ErrorResponseDto.forGeneralError(
            HttpStatus.UNAUTHORIZED.value(),
            "Credenciais inválidas. Por favor, verifique seu usuário e senha.",
            request.getRequestURI());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(AccountException.class)
  public ResponseEntity<ErrorResponseDto> handlerAccountLockedException(
      AccountException e, HttpServletRequest request) {

    ErrorResponseDto response =
        ErrorResponseDto.forGeneralError(
            HttpStatus.FORBIDDEN.value(), e.getMessage(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
  }
}

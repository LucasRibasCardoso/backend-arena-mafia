package com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.user.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.user.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.infrastructure.web.exceptionHandler.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {

  @ExceptionHandler(BadPhoneNumberException.class)
  public ResponseEntity<ErrorResponseDto> handlerBadPhoneNumberException(
      HttpServletRequest request, BadPhoneNumberException ex) {

    ErrorResponseDto response =
        ErrorResponseDto.forGeneralError(
            HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponseDto> handlerUserAlreadyExistsException(
      UserAlreadyExistsException ex, HttpServletRequest request) {

    ErrorResponseDto response =
        ErrorResponseDto.forGeneralError(
            HttpStatus.CONFLICT.value(), ex.getMessage(), request.getRequestURI());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }
}

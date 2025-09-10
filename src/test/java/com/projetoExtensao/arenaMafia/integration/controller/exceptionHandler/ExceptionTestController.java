package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendOtpRequestDto;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/exceptions")
public class ExceptionTestController {

  // --- Endpoints para 400 Bad Request ---
  @PostMapping("/bad-request/otp-session-invalid")
  public void throwOtpSessionInvalid(@Valid @RequestBody ResendOtpRequestDto dto) {}

  // --- Endpoints para 401 Unauthorized ---
  @GetMapping("/unauthorized/refresh-token-expired")
  public void throwRefreshTokenExpired() {
    throw new RefreshTokenExpiredException();
  }

  // --- Endpoints para 403 Forbidden ---
  @GetMapping("/forbidden/access-denied")
  public void throwAccessDenied() {
    throw new AccessDeniedException("Acesso negado.");
  }

  // --- Endpoints para 404 Not Found ---
  @GetMapping("/not-found/user-not-found")
  public void throwUserNotFound() {
    throw new UserNotFoundException();
  }

  // --- Endpoints para 409 Conflict ---
  @GetMapping("/conflict/user-already-exists")
  public void throwUserAlreadyExists() {
    throw new UserAlreadyExistsException(ErrorCode.USERNAME_ALREADY_EXISTS);
  }

  @GetMapping("/conflict/data-integrity")
  public void throwDataIntegrity() {
    throw new DataIntegrityViolationException("Violação de integridade de dados.");
  }

  // --- Endpoint para 500 Internal Server Error ---
  @GetMapping("/internal-server-error")
  public void throwGenericException() {
    throw new RuntimeException("Erro genérico simulado.");
  }
}

package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.UserAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/exceptions")
public class ExceptionTestController {

  // --- Endpoints para 400 Bad Request ---
  @PostMapping("/method-argument-not-valid")
  public void throwMethodArgumentNotValid(@Valid @RequestBody TestDto dto) {}

  @GetMapping("/bad-request/bad-phone-number")
  public void throwBadPhoneNumber() {
    throw new BadPhoneNumberException("Número de telefone inválido.");
  }

  // --- Endpoints para 401 Unauthorized ---
  @GetMapping("/unauthorized/bad-credentials")
  public void throwBadCredentials() {
    throw new BadCredentialsException("Credenciais inválidas.");
  }

  @GetMapping("/unauthorized/refresh-token-expired")
  public void throwRefreshTokenExpired() {
    throw new RefreshTokenExpiredException("Sessão expirada.");
  }

  // --- Endpoints para 403 Forbidden ---
  @GetMapping("/forbidden/access-denied")
  public void throwAccessDenied() {
    throw new AccessDeniedException("Acesso negado.");
  }

  // --- Endpoints para 404 Not Found ---
  @GetMapping("/not-found/user-not-found")
  public void throwUserNotFound() {
    throw new UserNotFoundException("Usuário não encontrado.");
  }

  // --- Endpoints para 409 Conflict ---
  @GetMapping("/conflict/user-already-exists")
  public void throwUserAlreadyExists() {
    throw new UserAlreadyExistsException("Usuário já existe.");
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

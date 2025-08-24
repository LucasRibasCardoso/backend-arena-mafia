package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler.utilsExceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.global.DomainValidationException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenInvalidFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.refreshToken.RefreshTokenNotFoundException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/exceptions")
public class TestExceptionHandlerController {

  @PostMapping("/validation")
  public void throwMethodArgumentNotValid(@Valid @RequestBody TestDto dto) {
    // A validação do @Valid vai lançar a exceção se o DTO for inválido
  }

  @GetMapping("/domain")
  public void throwDomainValidation() {
    // Simula um erro na camada de "Domain", exemplo: Dentro da classe User
    throw new DomainValidationException("Erro de domínio simulado.");
  }

  @GetMapping("/generic")
  public void throwGenericException() {
    // Simula um Internal Server error
    throw new RuntimeException();
  }

  @GetMapping("/data-integrity")
  public void throwDataIntegrity() {
    // Em um teste real, você pode mockar um serviço para lançar isso
    throw new DataIntegrityViolationException(
        "Erro de integridade de dados. Verifique os dados informados.");
  }

  @GetMapping("/access-denied")
  public void throwAccessDenied() {
    throw new AccessDeniedException("Acesso negado para acessar esse recurso");
  }

  @GetMapping("/badCredentials")
  public void throwBadCredentials() {
    throw new BadCredentialsException("Credenciais inválidas");
  }

  @GetMapping("/refresh-token-expired")
  public void throwBadRefreshToken() {
    throw new RefreshTokenExpiredException("Refresh token expirado");
  }

  @GetMapping("/refresh-not-found")
  public void throwNotFoundRefreshToken() {
    throw new RefreshTokenNotFoundException("Refresh token expirado");
  }

  @GetMapping("/invalid-format-refresh-token")
  public void throwInvalidFormatRefreshToken() {
    throw new RefreshTokenInvalidFormatException("Formato inválido do refresh token");
  }
}

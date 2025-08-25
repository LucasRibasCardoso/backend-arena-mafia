package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler.utilsExceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.global.DomainValidationException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/exceptions/global/")
public class TestGlobalExceptionHandlerController {

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
}

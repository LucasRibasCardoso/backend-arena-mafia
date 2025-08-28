package com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation;

/** Interface que define um contrato para DTOs que precisam de validação de confirmação de senha. */
public interface PasswordConfirmationProvider {
  String getPassword();

  String getConfirmPassword();
}

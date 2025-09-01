package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;

public enum AccountStatus {
  PENDING_VERIFICATION("pending_verification"),
  ACTIVE("active"),
  LOCKED("locked"),
  DISABLED("disabled");

  private final String value;

  AccountStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  public void validateEnabled() {
    switch (this) {
      case LOCKED ->
          throw new AccountStateConflictException(
              "Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
      case PENDING_VERIFICATION ->
          throw new AccountStateConflictException(
              "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
      case ACTIVE -> {}
      default -> throw new AccountStateConflictException("Atenção: Sua conta está desativada.");
    }
  }
}

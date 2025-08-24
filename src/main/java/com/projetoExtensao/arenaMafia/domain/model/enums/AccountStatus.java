package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountStatus {
  PENDING_VERIFICATION("pending_verification"),
  ACTIVE("active"),
  LOCKED("locked");

  private final String value;

  AccountStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}

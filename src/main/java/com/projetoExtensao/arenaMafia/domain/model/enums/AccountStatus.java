package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;

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
      case ACTIVE -> {}
      case LOCKED -> throw new AccountStatusForbiddenException(ErrorCode.ACCOUNT_LOCKED);
      case PENDING_VERIFICATION ->
          throw new AccountStatusForbiddenException(ErrorCode.ACCOUNT_PENDING_VERIFICATION);
      default -> throw new AccountStatusForbiddenException(ErrorCode.ACCOUNT_DISABLED);
    }
  }
}

package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStateConflictException;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import org.junit.jupiter.api.Test;

public class AccountStatusTest {

  @Test
  void validateEnabled_shouldNotThrow_whenActive() {
    assertThatCode(AccountStatus.ACTIVE::validateEnabled).doesNotThrowAnyException();
  }

  @Test
  void validateEnabled_shouldThrow_whenLocked() {
    assertThatThrownBy(AccountStatus.LOCKED::validateEnabled)
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está bloqueada. Por favor, contate o suporte.");
  }

  @Test
  void validateEnabled_shouldThrow_whenPendingVerification() {
    assertThatThrownBy(AccountStatus.PENDING_VERIFICATION::validateEnabled)
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage(
            "Atenção: Você precisa ativar sua conta. Por favor, termine o processo de cadastro.");
  }

  @Test
  void validateEnabled_shouldThrow_whenDisabled() {
    assertThatThrownBy(AccountStatus.DISABLED::validateEnabled)
        .isInstanceOf(AccountStateConflictException.class)
        .hasMessage("Atenção: Sua conta está desativada.");
  }
}

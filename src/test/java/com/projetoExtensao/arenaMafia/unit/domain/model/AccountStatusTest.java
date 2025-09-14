package com.projetoExtensao.arenaMafia.unit.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;

public class AccountStatusTest {

  @Test
  void validateEnabled_shouldNotThrow_whenActive() {
    assertThatCode(AccountStatus.ACTIVE::validateEnabled).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource(
      "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#accountStatusNonActiveProvider")
  @DisplayName("Deve lançar AccountStatusForbiddenException quando o status não for ACTIVE")
  void validateEnabled_shouldThrow_forInvalidStatuses(
      AccountStatus status, ErrorCode expectedErrorCode) {
    // Act & Assert
    assertThatThrownBy(status::validateEnabled)
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              AccountStatusForbiddenException exception =
                  (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode);
            }
        );
  }
}

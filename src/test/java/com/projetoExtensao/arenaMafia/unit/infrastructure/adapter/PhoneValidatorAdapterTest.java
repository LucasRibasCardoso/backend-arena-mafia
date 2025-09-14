package com.projetoExtensao.arenaMafia.unit.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.PhoneValidatorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Testes unitários para PhoneValidatorAdapter")
public class PhoneValidatorAdapterTest {

  private PhoneValidatorAdapter phoneValidatorAdapter;

  @BeforeEach
  void setUp() {
    this.phoneValidatorAdapter = new PhoneValidatorAdapter();
  }

  @Test
  @DisplayName("Deve formatar um número de telefone brasileiro válido para o formato E164")
  void formatToE164_shouldFormatValidBrazilianNumber() {
    // Arrange
    String validPhoneNumber = "+5547988776655";

    // Act
    String formattedNumber = phoneValidatorAdapter.formatToE164(validPhoneNumber);

    // Assert
    assertThat(formattedNumber).isEqualTo("+5547988776655");
  }

  @Test
  @DisplayName("Deve formatar um número de telefone dos EUA válido para o formato E164")
  void formatToE164_shouldFormatValidUSANumber() {
    // Arrange
    String validPhoneNumber = "+14155552671";

    // Act
    String formattedNumber = phoneValidatorAdapter.formatToE164(validPhoneNumber);

    // Assert
    assertThat(formattedNumber).isEqualTo("+14155552671");
  }

  @Test
  @DisplayName("Deve lançar InvalidFormatPhoneException quando o código de páis é mas inválido")
  void formatToE164_shouldThrowInvalidFormatPhoneException_whenPhoneIsParseableButIsInvalid() {
    // Arrange
    String invalidButParsableNumber = "+999 12345678";

    // Act & Assert
    assertThatThrownBy(() -> phoneValidatorAdapter.formatToE164(invalidButParsableNumber))
        .isInstanceOf(InvalidFormatPhoneException.class)
        .satisfies(
            ex -> {
              InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_INVALID);
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"abcde", "123"})
  @DisplayName("Deve lançar InvalidFormatPhoneException quando o número é inválido")
  void formatToE164_shouldThrowInvalidFormatPhoneException_whenPhoneFormatIsInvalid(
      String invalidNumber) {
    // Act & Assert
    assertThatThrownBy(() -> phoneValidatorAdapter.formatToE164(invalidNumber))
        .isInstanceOf(InvalidFormatPhoneException.class)
        .satisfies(
            ex -> {
              InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_INVALID);
            });
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Deve lançar InvalidFormatPhoneException quando o número é nulo ou vazio")
  void formatToE164_shouldThrowInvalidFormatPhoneException_whenPhoneIsNullOrEmpty(String phone) {
    // Act & Assert
    assertThatThrownBy(() -> phoneValidatorAdapter.formatToE164(phone))
        .isInstanceOf(InvalidFormatPhoneException.class)
        .satisfies(
            ex -> {
              InvalidFormatPhoneException exception = (InvalidFormatPhoneException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_REQUIRED);
            });
  }
}

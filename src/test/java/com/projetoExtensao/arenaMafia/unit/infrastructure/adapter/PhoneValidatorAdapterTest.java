package com.projetoExtensao.arenaMafia.unit.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.BadPhoneNumberException;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.PhoneValidatorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

  @ParameterizedTest
  @ValueSource(strings = {"+5547123", "+1202123", "999"}) // Números curtos demais
  @DisplayName(
      "Deve lançar BadPhoneNumberException para números que são válidos sintaticamente mas inválidos em regras")
  void formatToE164_shouldThrowExceptionForInvalidButParsableNumbers(String invalidNumber) {
    // Act & Assert
    assertThatThrownBy(() -> phoneValidatorAdapter.formatToE164(invalidNumber))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"+ABCDEFG", "47988887777", ""}) // Formatos impossíveis de analisar
  @DisplayName("Deve lançar BadPhoneNumberException para formatos de número inválidos")
  void formatToE164_shouldThrowExceptionForInvalidFormats(String invalidFormat) {
    // Act & Assert
    assertThatThrownBy(() -> phoneValidatorAdapter.formatToE164(invalidFormat))
        .isInstanceOf(BadPhoneNumberException.class)
        .hasMessage("Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
  }
}

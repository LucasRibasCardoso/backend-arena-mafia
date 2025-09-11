package com.projetoExtensao.arenaMafia.unit.domain.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Testes para o Value Object: OtpCode")
class OtpCodeTest {

  @Nested
  @DisplayName("Testes de criação e validação")
  class CreationTests {

    @Test
    @DisplayName("Deve criar um OtpCode com sucesso a partir de uma string válida")
    void fromString_shouldCreateInstance_whenValueIsValid() {
      // Arrange
      String validCode = "123456";

      // Act
      OtpCode otpCode = OtpCode.fromString(validCode);

      // Assert
      assertThat(otpCode).isNotNull();
      assertThat(otpCode.value()).isEqualTo(validCode);
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidOtpCodeProvider")
    @DisplayName("Deve lançar InvalidTokenFormatException quando o formato do  token for inválido")
    void fromString_shouldThrowException_whenFormatIsInvalid(
        String invalidCode, ErrorCode expectedError) {
      // Act & Assert
      assertThatThrownBy(() -> OtpCode.fromString(invalidCode))
          .isInstanceOf(InvalidOtpException.class)
          .satisfies(
              ex -> {
                InvalidOtpException exception = (InvalidOtpException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(expectedError);
              });
    }
  }

  @Nested
  @DisplayName("Testes do método generate")
  class GenerateTests {

    @Test
    @DisplayName("Deve gerar um OtpCode válido e não nulo")
    void generate_shouldCreateValidInstance() {
      // Act
      OtpCode otpCode = OtpCode.generate();

      // Assert
      assertThat(otpCode).isNotNull();
      assertThat(otpCode.value()).isNotNull();
      assertThat(otpCode.value()).matches("\\d{6}");
    }
  }

  @Nested
  @DisplayName("Testes do contrato de equals e hashCode")
  class EqualityTests {
    @Test
    @DisplayName("Dois OtpCodes com o mesmo valor devem ser iguais")
    void equals_shouldBeTrue_forSameValue() {
      // Arrange
      OtpCode code1 = OtpCode.fromString("987654");
      OtpCode code2 = OtpCode.fromString("987654");

      // Assert
      assertThat(code1).isEqualTo(code2);
      assertThat(code1).hasSameHashCodeAs(code2);
    }

    @Test
    @DisplayName("Dois OtpCodes com valores diferentes não devem ser iguais")
    void equals_shouldBeFalse_forDifferentValue() {
      // Arrange
      OtpCode code1 = OtpCode.fromString("111222");
      OtpCode code2 = OtpCode.fromString("333444");

      // Assert
      assertThat(code1).isNotEqualTo(code2);
    }
  }

  @Nested
  @DisplayName("Testes do método toString")
  class ToStringTests {
    @Test
    @DisplayName("Deve retornar o valor interno da string")
    void toString_shouldReturnInternalValue() {
      // Arrange
      String value = "123456";
      OtpCode otpCode = OtpCode.fromString(value);

      // Assert
      assertThat(otpCode.toString()).isEqualTo(value);
    }
  }
}

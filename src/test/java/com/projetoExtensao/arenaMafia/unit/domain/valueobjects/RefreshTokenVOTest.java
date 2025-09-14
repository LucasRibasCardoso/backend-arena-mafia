package com.projetoExtensao.arenaMafia.unit.domain.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Testes para o Value Object: RefreshTokenVO")
class RefreshTokenVOTest {

  @Nested
  @DisplayName("Testes de criação e validação")
  class CreationTests {

    @Test
    @DisplayName("Deve criar um RefreshTokenVO com sucesso a partir de uma string UUID válida")
    void fromString_shouldCreateInstance_whenValueIsValid() {
      // Arrange
      String validUuidString = "d3a8a3a0-4b7f-4b0e-8b1a-1e2b3c4d5e6f";
      UUID expectedUuid = UUID.fromString(validUuidString);

      // Act
      RefreshTokenVO token = RefreshTokenVO.fromString(validUuidString);

      // Assert
      assertThat(token).isNotNull();
      assertThat(token.value()).isEqualTo(expectedUuid);
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestDataProvider#invalidRefreshTokenProvider")
    @DisplayName("Deve lançar InvalidTokenFormatException quando o formato do  token for inválido")
    void fromString_shouldThrowException_whenValueIsNullOrBlank(
        String invalidToken, ErrorCode expectedError) {
      // Act & Assert
      assertThatThrownBy(() -> RefreshTokenVO.fromString(invalidToken))
          .isInstanceOf(InvalidTokenFormatException.class)
          .satisfies(
              ex -> {
                InvalidTokenFormatException exception = (InvalidTokenFormatException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(expectedError);
              });
    }
  }

  @Nested
  @DisplayName("Testes do método generate")
  class GenerateTests {

    @Test
    @DisplayName("Deve gerar um RefreshTokenVO válido e não nulo")
    void generate_shouldCreateValidInstance() {
      // Act
      RefreshTokenVO token = RefreshTokenVO.generate();

      // Assert
      assertThat(token).isNotNull();
      assertThat(token.value()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Testes do contrato de equals e hashCode")
  class EqualityTests {

    private final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    @Test
    @DisplayName("Dois tokens com o mesmo UUID devem ser iguais")
    void equals_shouldBeTrue_forSameValue() {
      RefreshTokenVO token1 = new RefreshTokenVO(uuid1);
      RefreshTokenVO token2 = new RefreshTokenVO(uuid1);

      assertThat(token1).isEqualTo(token2);
      assertThat(token1).hasSameHashCodeAs(token2);
    }

    @Test
    @DisplayName("Dois tokens com UUIDs diferentes não devem ser iguais")
    void equals_shouldBeFalse_forDifferentValue() {
      RefreshTokenVO token1 = new RefreshTokenVO(uuid1);
      RefreshTokenVO token2 = new RefreshTokenVO(uuid2);

      assertThat(token1).isNotEqualTo(token2);
    }
  }

  @Nested
  @DisplayName("Testes do método toString")
  class ToStringTests {
    @Test
    @DisplayName("Deve retornar a representação em string do UUID interno")
    void toString_shouldReturnInternalUuidAsString() {
      String uuidString = "a1b2c3d4-e5f6-a1b2-c3d4-e5f6a1b2c3d4";
      RefreshTokenVO token = RefreshTokenVO.fromString(uuidString);

      assertThat(token.toString()).isEqualTo(uuidString);
    }
  }
}

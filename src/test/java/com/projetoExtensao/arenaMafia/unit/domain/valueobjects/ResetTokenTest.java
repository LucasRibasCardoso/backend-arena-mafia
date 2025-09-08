package com.projetoExtensao.arenaMafia.unit.domain.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Testes para o Value Object: ResetToken")
class ResetTokenTest {

  @Nested
  @DisplayName("Testes de criação e validação")
  class CreationTests {

    @Test
    @DisplayName("Deve criar um ResetToken com sucesso a partir de uma string UUID válida")
    void fromString_shouldCreateInstance_whenValueIsValid() {
      // Arrange
      String validUuidString = "d3a8a3a0-4b7f-4b0e-8b1a-1e2b3c4d5e6f";
      UUID expectedUuid = UUID.fromString(validUuidString);

      // Act
      ResetToken token = ResetToken.fromString(validUuidString);

      // Assert
      assertThat(token).isNotNull();
      assertThat(token.value()).isEqualTo(expectedUuid);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Deve lançar InvalidTokenFormatException para strings nulas, vazias ou em branco")
    void fromString_shouldThrowException_whenValueIsNullOrBlank(String invalidToken) {
      // Act & Assert
      assertThatThrownBy(() -> ResetToken.fromString(invalidToken))
          .isInstanceOf(InvalidTokenFormatException.class)
          .hasMessage("O token de redefinição de senha não pode ser nulo ou vazio.");
    }

    @Test
    @DisplayName("Deve lançar InvalidTokenFormatException para string com formato de UUID inválido")
    void fromString_shouldThrowException_whenUuidFormatIsInvalid() {
      // Arrange
      String malformedUuid = "not-a-valid-uuid";

      // Act & Assert
      assertThatThrownBy(() -> ResetToken.fromString(malformedUuid))
          .isInstanceOf(InvalidTokenFormatException.class)
          .hasMessage("Formato inválido para o token de redefinição de senha.");
    }

    @Test
    @DisplayName(
        "Deve lançar InvalidTokenFormatException ao tentar criar com um UUID nulo diretamente")
    void constructor_shouldThrowException_whenUuidIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> new ResetToken(null))
          .isInstanceOf(InvalidTokenFormatException.class)
          .hasMessage("O token de redefinição de senha não pode ser nulo.");
    }
  }

  @Nested
  @DisplayName("Testes do método generate")
  class GenerateTests {

    @Test
    @DisplayName("Deve gerar um ResetToken válido e não nulo")
    void generate_shouldCreateValidInstance() {
      // Act
      ResetToken token = ResetToken.generate();

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
      ResetToken token1 = new ResetToken(uuid1);
      ResetToken token2 = new ResetToken(uuid1);

      assertThat(token1).isEqualTo(token2);
      assertThat(token1).hasSameHashCodeAs(token2);
    }

    @Test
    @DisplayName("Dois tokens com UUIDs diferentes não devem ser iguais")
    void equals_shouldBeFalse_forDifferentValue() {
      ResetToken token1 = new ResetToken(uuid1);
      ResetToken token2 = new ResetToken(uuid2);

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
      ResetToken token = ResetToken.fromString(uuidString);

      assertThat(token.toString()).isEqualTo(uuidString);
    }
  }
}

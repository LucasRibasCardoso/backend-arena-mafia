package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTokenFormatException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Testes unitários para RefreshTokenVO")
public class RefreshTokenVoTest {

  @Test
  @DisplayName("Deve gerar um novo RefreshTokenVO com sucesso")
  void generate_shouldCreateNewToken() {
    // Act
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.generate();

    // Assert
    assertThat(refreshTokenVO).isNotNull();
    assertThat(refreshTokenVO.value()).isNotNull();
  }

  @Test
  @DisplayName(
      "Deve lançar RefreshTokenInvalidFormatException ao tentar criar um RefreshTokenVO via construtor com valor nulo")
  void constructor_shouldThrowExceptionForNullValue() {
    // Act & Assert
    assertThatThrownBy(() -> new RefreshTokenVO(null))
        .isInstanceOf(InvalidTokenFormatException.class)
        .hasMessage("Refresh token não pode ser nulo.");
  }

  @Nested
  @DisplayName("Testes para o método fromString")
  class FromStringTests {
    @Test
    @DisplayName("Deve criar um RefreshTokenVO a partir de uma string UUID válida")
    void fromString_shouldCreateTokenFromValidString() {
      // Arrange
      String validUuidString = "d3b4b3b4-1b3b-4b3b-8b3b-1b3b3b3b3b3b";
      UUID expectedUuid = UUID.fromString(validUuidString);

      // Act
      RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(validUuidString);

      // Assert
      assertThat(refreshTokenVO).isNotNull();
      assertThat(refreshTokenVO.value()).isEqualTo(expectedUuid);
    }

    @Test
    @DisplayName(
        "Deve lançar RefreshTokenInvalidFormatException para uma string com formato inválido")
    void fromString_shouldThrowExceptionForInvalidStringFormat() {
      // Arrange
      String invalidUuidString = "isso-nao-e-um-uuid";

      // Act & Assert
      assertThatThrownBy(() -> RefreshTokenVO.fromString(invalidUuidString))
          .isInstanceOf(InvalidTokenFormatException.class)
          .hasMessageContaining("Formato inválido para o refresh token.");
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException quando a string for vazia ou nula")
    void fromString_shouldThrowExceptionForNullValue() {
      // Act & Assert
      assertThatThrownBy(() -> RefreshTokenVO.fromString(null))
          .isInstanceOf(InvalidTokenFormatException.class)
          .hasMessage("Refresh token não pode ser nulo ou vazio.");
    }
  }

  @Test
  @DisplayName("toString deve retornar a representação em string do UUID")
  void toString_shouldReturnUuidString() {
    // Arrange
    String uuidString = "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d";
    RefreshTokenVO refreshTokenVO = RefreshTokenVO.fromString(uuidString);

    // Act
    String result = refreshTokenVO.toString();

    // Assert
    assertThat(result).isEqualTo(uuidString);
  }
}

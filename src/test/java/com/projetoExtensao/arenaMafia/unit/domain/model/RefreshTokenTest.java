package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Testes unitários para entidade RefreshToken")
public class RefreshTokenTest {

  private User testUser;
  private final Long EXPIRATION_DAYS = 30L;

  @BeforeEach
  void setUp() {
    testUser = User.create("testuser", "Test User", "5547912345678", "hash");
  }

  @Test
  @DisplayName("Deve criar um RefreshToken com sucesso usando o factory method")
  void create_shouldCreateTokenSuccessfully() {
    // Arrange
    Instant startTime = Instant.now();

    // Act
    RefreshToken refreshToken = RefreshToken.create(EXPIRATION_DAYS, testUser);

    // Assert
    assertThat(refreshToken).isNotNull();
    assertThat(refreshToken.getToken()).isNotNull();
    assertThat(refreshToken.getUser()).isEqualTo(testUser);
    assertThat(refreshToken.getCreatedAt()).isAfterOrEqualTo(startTime);

    // Verifica se a data de expiração está aproximadamente 30 dias no futuro
    Instant expectedExpiryDate = startTime.plus(EXPIRATION_DAYS, ChronoUnit.DAYS);
    assertThat(refreshToken.getExpiryDate()).isAfterOrEqualTo(expectedExpiryDate);
  }

  @Test
  @DisplayName("Deve reconstituir um RefreshToken com sucesso a partir de dados existentes")
  void reconstitute_shouldRebuildTokenSuccessfully() {
    // Arrange
    RefreshTokenVO tokenVO = RefreshTokenVO.generate();
    Instant expiryDate = Instant.now().plus(15, ChronoUnit.DAYS);
    Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);

    // Act
    RefreshToken refreshToken = RefreshToken.reconstitute(tokenVO, expiryDate, testUser, createdAt);

    // Assert
    assertThat(refreshToken.getToken()).isEqualTo(tokenVO);
    assertThat(refreshToken.getExpiryDate()).isEqualTo(expiryDate);
    assertThat(refreshToken.getUser()).isEqualTo(testUser);
    assertThat(refreshToken.getCreatedAt()).isEqualTo(createdAt);
  }

  @Nested
  @DisplayName("Testes para o método verifyIfNotExpired")
  class verifyIfNotExpiredTests {

    @Test
    @DisplayName("verifyIfNotExpired não deve lançar exceção para um token que ainda é válido")
    void verifyIfNotExpired_shouldNotThrowExceptionForValidToken() {
      // Arrange
      RefreshToken validToken = RefreshToken.create(EXPIRATION_DAYS, testUser);

      // Act & Assert
      assertDoesNotThrow(validToken::verifyIfNotExpired);
    }

    @Test
    @DisplayName(
        "verifyIfNotExpired deve lançar RefreshTokenExpiredException para um token que já expirou")
    void verifyIfNotExpired_shouldThrowExceptionForExpiredToken() {
      // Arrange
      RefreshToken expiredToken = RefreshToken.create(-1L, testUser);

      // Act & Assert
      assertThatThrownBy(() -> expiredToken.verifyIfNotExpired())
          .isInstanceOf(RefreshTokenExpiredException.class)
          .hasMessage("Sua sessão expirou. Por favor, faça login novamente.");
    }

    @Test
    @DisplayName("verifyIfNotExpired deve lançar exceção para um token que expirou no passado")
    void verifyIfNotExpired_shouldThrowExceptionForTokenThatExpiredInThePast() {
      // Arrange
      // Data de expiração que é 1 segundo no passado para garantir a falha.
      Instant expiredInstant = Instant.now().minusSeconds(1);
      RefreshToken expiredToken =
          RefreshToken.reconstitute(
              RefreshTokenVO.generate(), expiredInstant, testUser, Instant.now());

      // Act & Assert
      assertThatThrownBy(() -> expiredToken.verifyIfNotExpired())
          .isInstanceOf(RefreshTokenExpiredException.class)
          .hasMessage("Sua sessão expirou. Por favor, faça login novamente.");
    }
  }
}

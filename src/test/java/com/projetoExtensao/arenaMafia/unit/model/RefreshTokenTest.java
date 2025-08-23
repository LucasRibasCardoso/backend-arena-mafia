package com.projetoExtensao.arenaMafia.unit.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  @DisplayName("Testes para o método isExpired")
  class IsExpiredTests {

    @Test
    @DisplayName("isExpired deve retornar false para um token que ainda é válido")
    void isExpired_shouldReturnFalseForValidToken() {
      // Arrange
      RefreshToken validToken = RefreshToken.create(EXPIRATION_DAYS, testUser);

      // Act & Assert
      assertFalse(validToken.isExpired());
    }

    @Test
    @DisplayName("isExpired deve retornar true para um token que já expirou")
    void isExpired_shouldReturnTrueForExpiredToken() {
      // Arrange
      RefreshToken expiredToken = RefreshToken.create(-1L, testUser);

      // Act & Assert
      assertTrue(expiredToken.isExpired());
    }

    @Test
    @DisplayName("isExpired deve retornar true para um token que expira exatamente agora")
    void isExpired_shouldReturnTrueForTokenExpiringNow() {
      // Arrange
      Instant now = Instant.now();
      RefreshToken tokenExpiringNow =
          RefreshToken.reconstitute(RefreshTokenVO.generate(), now, testUser, now);

      // Act & Assert
      assertTrue(tokenExpiringNow.isExpired());
    }
  }
}

package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.RefreshTokenExpiredException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Testes unitários para entidade RefreshToken")
public class RefreshTokenTest {

  private final Long EXPIRATION_DAYS = 30L;

  @Test
  @DisplayName("Deve criar um RefreshToken com sucesso usando o factory method")
  void create_shouldCreateTokenSuccessfully() {
    // Arrange
    Instant startTime = Instant.now();
    User user = TestDataProvider.createActiveUser();

    // Act
    RefreshToken refreshToken = RefreshToken.create(EXPIRATION_DAYS, user);

    // Assert
    assertThat(refreshToken).isNotNull();
    assertThat(refreshToken.getToken()).isNotNull();
    assertThat(refreshToken.getUser()).isEqualTo(user);
    assertThat(refreshToken.getCreatedAt()).isAfterOrEqualTo(startTime);

    // Verifica se a data de expiração está aproximadamente 30 dias no futuro
    Instant expectedExpiryDate = startTime.plus(EXPIRATION_DAYS, ChronoUnit.DAYS);
    assertThat(refreshToken.getExpiryDate()).isAfterOrEqualTo(expectedExpiryDate);
  }

  @Test
  @DisplayName("Deve reconstituir um RefreshToken com sucesso a partir de dados existentes")
  void reconstitute_shouldRebuildTokenSuccessfully() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    RefreshTokenVO tokenVO = RefreshTokenVO.generate();
    Instant expiryDate = Instant.now().plus(15, ChronoUnit.DAYS);
    Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);

    // Act
    RefreshToken refreshToken = RefreshToken.reconstitute(1L, tokenVO, expiryDate, user, createdAt);

    // Assert
    assertThat(refreshToken.getToken()).isEqualTo(tokenVO);
    assertThat(refreshToken.getExpiryDate()).isEqualTo(expiryDate);
    assertThat(refreshToken.getUser()).isEqualTo(user);
    assertThat(refreshToken.getCreatedAt()).isEqualTo(createdAt);
  }

  @Nested
  @DisplayName("Testes para o método verifyIfNotExpired")
  class verifyIfNotExpiredTests {

    @Test
    @DisplayName("verifyIfNotExpired não deve lançar exceção para um token que ainda é válido")
    void verifyIfNotExpired_shouldNotThrowExceptionForValidToken() {
      // Arrange
      User user = TestDataProvider.createActiveUser();
      RefreshToken validToken = RefreshToken.create(EXPIRATION_DAYS, user);

      // Act & Assert
      assertDoesNotThrow(validToken::verifyIfNotExpired);
    }

    @Test
    @DisplayName(
        "verifyIfNotExpired deve lançar RefreshTokenExpiredException para um token que já expirou")
    void verifyIfNotExpired_shouldThrowExceptionForExpiredToken() {
      // Arrange
      User user = TestDataProvider.createActiveUser();
      RefreshToken expiredToken = TestDataProvider.createExpiredRefreshToken(user);

      // Act & Assert
      assertThatThrownBy(expiredToken::verifyIfNotExpired)
          .isInstanceOf(RefreshTokenExpiredException.class)
          .satisfies(
              ex -> {
                RefreshTokenExpiredException exception = (RefreshTokenExpiredException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);
              });
    }

    @Test
    @DisplayName("verifyIfNotExpired deve lançar exceção para um token que expirou no passado")
    void verifyIfNotExpired_shouldThrowExceptionForTokenThatExpiredInThePast() {
      // Arrange
      User user = TestDataProvider.createActiveUser();
      RefreshToken expiredToken = TestDataProvider.createExpiredRefreshToken(user);

      // Act & Assert
      assertThatThrownBy(expiredToken::verifyIfNotExpired)
          .isInstanceOf(RefreshTokenExpiredException.class)
          .satisfies(
              ex -> {
                RefreshTokenExpiredException exception = (RefreshTokenExpiredException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);
              });
    }
  }
}

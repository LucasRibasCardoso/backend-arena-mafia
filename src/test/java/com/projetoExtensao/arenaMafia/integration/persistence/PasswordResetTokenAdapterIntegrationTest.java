package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.PasswordResetTokenAdapter;
import com.projetoExtensao.arenaMafia.integration.config.BaseTestContainersConfig;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@DisplayName("Testes de integração de persistência para PasswordResetTokenAdapter")
public class PasswordResetTokenAdapterIntegrationTest extends BaseTestContainersConfig {

  @Autowired private PasswordResetTokenAdapter passwordResetTokenAdapter;
  @Autowired private RedisTemplate<String, String> redisTemplate;

  private final String TOKEN_PREFIX = "password-reset-token:";

  @Test
  @DisplayName("Deve salvar o token de redefinição no Redis com o userId e expiração corretos")
  void save_shouldSaveTokenInRedisWithCorrectUserIdAndExpiration() {
    // Arrange
    UUID userId = UUID.randomUUID();

    // Act
    String generatedToken = passwordResetTokenAdapter.save(userId);

    // Assert
    assertThat(generatedToken).isNotNull().hasSize(36); // Tamanho de um UUID

    String redisKey = TOKEN_PREFIX + generatedToken;
    String storedUserId = redisTemplate.opsForValue().get(redisKey);
    Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

    assertThat(storedUserId).isEqualTo(userId.toString());
    assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
  }

  @Nested
  @DisplayName("Testes para o método getUserIdByTokenOrElseThrow")
  class FindUserIdByTokenTests {

    @Test
    @DisplayName("Deve encontrar e retornar o userId para um token válido e existente")
    void getUserIdByTokenOrElseThrow_shouldReturnUserId_whenTokenExists() {
      // Arrange
      UUID userId = UUID.randomUUID();
      String token = UUID.randomUUID().toString();
      String redisKey = TOKEN_PREFIX + token;
      redisTemplate.opsForValue().set(redisKey, userId.toString(), Duration.ofMinutes(5));

      // Act
      Optional<UUID> foundUserId = passwordResetTokenAdapter.findUserIdByResetToken(token);

      // Assert
      assertThat(foundUserId).isPresent();
      assertThat(foundUserId.get()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando o token não existir")
    void getUserIdByTokenOrElseThrow_shouldThrowException_whenTokenDoesNotExist() {
      // Arrange
      String nonExistentToken = UUID.randomUUID().toString();

      // Act
      Optional<UUID> foundUserId =
          passwordResetTokenAdapter.findUserIdByResetToken(nonExistentToken);

      // Assert
      assertThat(foundUserId).isNotPresent();
    }
  }

  @Test
  @DisplayName("Deve deletar um token existente do Redis")
  void delete_shouldRemoveTokenFromRedis() {
    // Arrange
    String token = UUID.randomUUID().toString();
    String redisKey = TOKEN_PREFIX + token;
    redisTemplate.opsForValue().set(redisKey, UUID.randomUUID().toString());

    // Garante que a chave existe ANTES de deletar
    assertThat(redisTemplate.hasKey(redisKey)).isTrue();

    // Act
    passwordResetTokenAdapter.delete(token);

    // Assert
    assertThat(redisTemplate.hasKey(redisKey)).isFalse();
  }
}

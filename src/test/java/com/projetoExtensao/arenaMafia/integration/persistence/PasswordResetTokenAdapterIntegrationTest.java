package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.PasswordResetTokenAdapter;
import com.projetoExtensao.arenaMafia.integration.config.BaseTestContainersConfig;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração de persistência para PasswordResetTokenAdapter")
public class PasswordResetTokenAdapterIntegrationTest extends BaseTestContainersConfig {

  @Autowired private PasswordResetTokenAdapter passwordResetTokenAdapter;
  @Autowired private RedisTemplate<String, String> redisTemplate;

  private final String TOKEN_PREFIX = "password-reset-token:";

  @Test
  @DisplayName("Deve gerar e salvar o token de redefinição no Redis com expiração")
  void generateToken_shouldSaveTokenInRedisWithCorrectUserIdAndExpiration() {
    // Arrange
    UUID userId = UUID.randomUUID();

    // Act
    ResetToken generatedToken = passwordResetTokenAdapter.generateToken(userId);

    // Assert
    assertThat(generatedToken).isNotNull();
    assertThat(generatedToken.value()).isNotNull();

    String redisKey = TOKEN_PREFIX + generatedToken;
    String storedUserId = redisTemplate.opsForValue().get(redisKey);
    Long ttl = redisTemplate.getExpire(redisKey);

    assertThat(storedUserId).isEqualTo(userId.toString());
    assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
  }

  @Nested
  @DisplayName("Testes para o método findUserIdByResetToken")
  class FindUserIdByResetTokenTests {

    @Test
    @DisplayName("Deve encontrar e retornar o UUID do usuário para um token válido e existente")
    void findUserIdByResetToken_shouldReturnUserId_whenTokenExists() {
      // Arrange
      UUID userId = UUID.randomUUID();
      ResetToken token = passwordResetTokenAdapter.generateToken(userId);

      // Act
      Optional<UUID> foundUserId = passwordResetTokenAdapter.findUserIdByResetToken(token);

      // Assert
      assertThat(foundUserId).isPresent();
      assertThat(foundUserId.get()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando o token não existir")
    void findUserIdByResetToken_shouldReturnEmpty_whenTokenDoesNotExist() {
      // Arrange
      ResetToken nonExistentToken = ResetToken.generate();

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
    UUID userId = UUID.randomUUID();
    ResetToken token = passwordResetTokenAdapter.generateToken(userId);
    String redisKey = TOKEN_PREFIX + token;

    assertThat(redisTemplate.hasKey(redisKey)).isTrue();

    // Act
    passwordResetTokenAdapter.delete(token);

    // Assert
    assertThat(redisTemplate.hasKey(redisKey)).isFalse();
  }
}

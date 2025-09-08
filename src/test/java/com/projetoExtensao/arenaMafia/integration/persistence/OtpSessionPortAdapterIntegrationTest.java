package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.OtpSessionPortAdapter;
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
@DisplayName("Testes de integração de persistência para OtpSessionPortAdapter")
public class OtpSessionPortAdapterIntegrationTest extends BaseTestContainersConfig {

  private static final String SESSION_PREFIX = "otp-session:";

  @Autowired private OtpSessionPortAdapter otpSessionPortAdapter;
  @Autowired private RedisTemplate<String, String> redisTemplate;

  @Nested
  @DisplayName("Testes para o método generateOtpSession")
  class GenerateOtpSessionTests {

    @Test
    @DisplayName("Deve gerar e salvar a sessão OTP com o userId e expiração no Redis")
    void generateOtpSession_shouldGenerateAndSaveSessionInRedis() {
      // Arrange
      UUID userId = UUID.randomUUID();

      // Act
      OtpSessionId generatedSessionId = otpSessionPortAdapter.generateOtpSession(userId);

      // Assert
      assertThat(generatedSessionId).isNotNull();
      assertThat(generatedSessionId.value()).isNotNull();

      String redisKey = SESSION_PREFIX + generatedSessionId;
      String storedUserId = redisTemplate.opsForValue().get(redisKey);
      Long ttl = redisTemplate.getExpire(redisKey);

      assertThat(storedUserId).isEqualTo(userId.toString());
      assertThat(ttl)
          .isNotNull()
          .isPositive()
          .isLessThanOrEqualTo(Duration.ofMinutes(10).toSeconds());
    }
  }

  @Nested
  @DisplayName("Testes para o método findUserIdByOtpSessionId")
  class FindUserIdTests {

    @Test
    @DisplayName("Deve retornar o UUID do usuário quando a sessão OTP existe")
    void findUserIdByOtpSessionId_shouldReturnUserId_whenSessionExists() {
      // Arrange
      UUID expectedUserId = UUID.randomUUID();
      OtpSessionId savedSessionId = otpSessionPortAdapter.generateOtpSession(expectedUserId);

      // Act
      Optional<UUID> foundUserIdOptional =
          otpSessionPortAdapter.findUserIdByOtpSessionId(savedSessionId);

      // Assert
      assertThat(foundUserIdOptional).isPresent();
      assertThat(foundUserIdOptional.get()).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("Deve retornar Optional.empty quando a sessão OTP não existe ou expirou")
    void findUserIdByOtpSessionId_shouldReturnEmpty_whenSessionDoesNotExist() {
      // Arrange
      OtpSessionId nonExistentSessionId = OtpSessionId.generate();

      // Act
      Optional<UUID> foundUserIdOptional =
          otpSessionPortAdapter.findUserIdByOtpSessionId(nonExistentSessionId);

      // Assert
      assertThat(foundUserIdOptional).isEmpty();
    }
  }
}

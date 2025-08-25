package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.OtpAdapter;
import com.projetoExtensao.arenaMafia.integration.config.TestContainerRedisConfig;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@DisplayName("Testes de integração de persistência para OtpAdapter")
public class OtpAdapterTest extends TestContainerRedisConfig {

  @Autowired private OtpAdapter otpAdapter;

  @Autowired private RedisTemplate<String, String> redisTemplate;

  @Test
  @DisplayName("Deve gerar e salvar o código OTP com expiração no Redis")
  void generateAndSaveOtp_shouldSaveOtpWithExpirationInRedis() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = "otp:user:" + userId;

    // Act
    String otpCode = otpAdapter.generateAndSaveOtp(userId);

    // Assert
    assertThat(otpCode).hasSize(6).containsOnlyDigits();

    String storedOtp = redisTemplate.opsForValue().get(redisKey);
    Long ttl = redisTemplate.getExpire(redisKey);

    assertThat(storedOtp).isEqualTo(otpCode);
    assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
  }

  @Test
  @DisplayName("Deve validar o código OTP corretamente e remover a chave do Redis após a validação")
  void validateOtp_shouldReturnTrueAndDeleteKey_whenOtpIsCorrect() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = "otp:user:" + userId;
    String otpCode = otpAdapter.generateAndSaveOtp(userId);

    // Act
    boolean isValid = otpAdapter.validateOtp(userId, otpCode);

    // Assert
    assertThat(isValid).isTrue();

    Boolean keyExists = redisTemplate.hasKey(redisKey);
    assertThat(keyExists).isFalse();
  }

  @Test
  @DisplayName("Deve retornar falso ao validar um código OTP incorreto e manter a chave no Redis")
  void validateOtp_shouldReturnFalse_whenOtpIsIncorrect() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = "otp:user:" + userId;
    otpAdapter.generateAndSaveOtp(userId);

    // Act
    boolean isValid = otpAdapter.validateOtp(userId, "000000");

    // Assert
    assertThat(isValid).isFalse();

    Boolean keyExists = redisTemplate.hasKey(redisKey);
    assertThat(keyExists).isTrue();
  }
}

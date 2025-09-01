package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.OtpAdapter;
import com.projetoExtensao.arenaMafia.integration.config.BaseTestContainersConfig;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração de persistência para OtpAdapter")
public class OtpAdapterIntegrationTest extends BaseTestContainersConfig {

  private static final String OTP_PREFIX = "otp-user:";

  @Autowired private OtpAdapter otpAdapter;

  @Autowired private RedisTemplate<String, String> redisTemplate;

  @Test
  @DisplayName("Deve gerar e salvar o código OTP com expiração no Redis")
  void generateCodeOTPWithExpirationInRedis() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = OTP_PREFIX + userId;

    // Act
    String otpCode = otpAdapter.generateCodeOTP(userId);

    // Assert
    assertThat(otpCode).hasSize(6).containsOnlyDigits();

    String storedOtp = redisTemplate.opsForValue().get(redisKey);
    Long ttl = redisTemplate.getExpire(redisKey);

    assertThat(storedOtp).isEqualTo(otpCode);
    assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
  }

  @Test
  @DisplayName(
      "Deve validar o código OTP corretamente e não lançar exceção, removendo a chave do Redis")
  void validateOtp_shouldNotThrowExceptionAndDeleteKey_whenOtpIsCorrect() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = "otp:user:" + userId;
    String otpCode = otpAdapter.generateCodeOTP(userId);

    // Act & Assert
    assertDoesNotThrow(() -> otpAdapter.validateOtp(userId, otpCode));

    Boolean keyExists = redisTemplate.hasKey(redisKey);
    assertThat(keyExists).isFalse();
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException ao validar um código OTP incorreto")
  void validateOtp_shouldThrowException_whenOtpIsIncorrect() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = OTP_PREFIX + userId;
    otpAdapter.generateCodeOTP(userId);
    String incorrectCode = "000000";

    // Act & Assert
    assertThatThrownBy(() -> otpAdapter.validateOtp(userId, incorrectCode))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage("Código de verificação inválido ou expirado.");

    Boolean keyExists = redisTemplate.hasKey(redisKey);
    assertThat(keyExists).isTrue();
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException ao validar um código OTP expirado")
  void validateOtp_shouldThrowException_whenOtpIsExpired() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = OTP_PREFIX + userId;
    String otpCode = otpAdapter.generateCodeOTP(userId);

    // Simula a expiração removendo a chave diretamente do Redis
    redisTemplate.delete(redisKey);

    // Act & Assert
    assertThatThrownBy(() -> otpAdapter.validateOtp(userId, otpCode))
        .isInstanceOf(InvalidOtpException.class)
        .hasMessage("Código de verificação inválido ou expirado.");
  }
}

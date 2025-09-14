package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
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
  void generateAndSaveOtp_shouldSaveOtpWithExpirationInRedis() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = OTP_PREFIX + userId;

    // Act
    OtpCode otpCode = otpAdapter.generateOtpCode(userId);

    // Assert
    assertThat(otpCode.value()).hasSize(6).containsOnlyDigits();

    String storedOtp = redisTemplate.opsForValue().get(redisKey);
    Long ttl = redisTemplate.getExpire(redisKey);

    assertThat(storedOtp).isEqualTo(otpCode.value());
    assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
  }

  @Test
  @DisplayName("Deve validar o código OTP e remover a chave do Redis")
  void validateOtp_shouldNotThrowExceptionAndDeleteKey_whenOtpIsCorrect() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = OTP_PREFIX + userId;
    OtpCode otpCode = otpAdapter.generateOtpCode(userId);

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
    otpAdapter.generateOtpCode(userId);
    OtpCode incorrectCode = OtpCode.fromString("000000");

    // Act & Assert
    assertThatThrownBy(() -> otpAdapter.validateOtp(userId, incorrectCode))
        .isInstanceOf(InvalidOtpException.class)
        .satisfies(
            ex -> {
              InvalidOtpException exception = (InvalidOtpException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OTP_CODE_INCORRECT_OR_EXPIRED);
            });

    Boolean keyExists = redisTemplate.hasKey(redisKey);
    assertThat(keyExists).isTrue();
  }

  @Test
  @DisplayName("Deve lançar InvalidOtpException ao validar um código OTP expirado (não encontrado)")
  void validateOtp_shouldThrowException_whenOtpIsExpired() {
    // Arrange
    UUID userId = UUID.randomUUID();
    String redisKey = OTP_PREFIX + userId;
    OtpCode otpCode = otpAdapter.generateOtpCode(userId);
    redisTemplate.delete(redisKey);

    // Act & Assert
    assertThatThrownBy(() -> otpAdapter.validateOtp(userId, otpCode))
        .isInstanceOf(InvalidOtpException.class)
        .satisfies(
            ex -> {
              InvalidOtpException exception = (InvalidOtpException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OTP_CODE_INCORRECT_OR_EXPIRED);
            });
  }
}

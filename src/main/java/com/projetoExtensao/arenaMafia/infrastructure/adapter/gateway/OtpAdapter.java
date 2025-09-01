package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OtpAdapter implements OtpPort {

  private static final String OTP_PREFIX = "otp-user:";
  private static final Duration OTP_EXPIRATION = Duration.ofMinutes(5);

  private final RedisTemplate<String, String> redisTemplate;
  private final SecureRandom secureRandom;

  public OtpAdapter(RedisTemplate<String, String> redisTemplate, SecureRandom secureRandom) {
    this.secureRandom = secureRandom;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public String generateCodeOTP(UUID userId) {
    String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
    redisTemplate.opsForValue().set(key(userId), otp, OTP_EXPIRATION);
    return otp;
  }

  @Override
  public void validateOtp(UUID userId, String code) {
    String stored = redisTemplate.opsForValue().get(key(userId));
    if (stored == null || !stored.equals(code)) {
      throw new InvalidOtpException("Código de verificação inválido ou expirado.");
    }
    redisTemplate.delete(key(userId));
  }

  private String key(UUID userId) {
    return OTP_PREFIX + userId;
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OtpAdapter implements OtpPort {

  private static final String OTP_PREFIX = "otp-user:";
  private static final Duration OTP_EXPIRATION = Duration.ofMinutes(5);

  private final RedisTemplate<String, String> redisTemplate;

  public OtpAdapter(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public OtpCode generateOtpCode(UUID userId) {
    OtpCode otpCode = OtpCode.generate();
    redisTemplate.opsForValue().set(key(userId), otpCode.value(), OTP_EXPIRATION);
    return otpCode;
  }

  @Override
  public void validateOtp(UUID userId, OtpCode otpCode) {
    String stored = redisTemplate.opsForValue().get(key(userId));
    if (stored == null || !stored.equals(otpCode.value())) {
      throw new InvalidOtpException(ErrorCode.OTP_CODE_INVALID_OR_EXPIRED);
    }
    redisTemplate.delete(key(userId));
  }

  private String key(UUID userId) {
    return OTP_PREFIX + userId;
  }
}

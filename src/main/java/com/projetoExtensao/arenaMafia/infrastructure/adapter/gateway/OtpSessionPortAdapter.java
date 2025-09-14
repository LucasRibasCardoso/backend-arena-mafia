package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OtpSessionPortAdapter implements OtpSessionPort {

  private static final String SESSION_PREFIX = "otp-session:";
  private static final Duration SESSION_EXPIRATION = Duration.ofMinutes(10);

  private final RedisTemplate<String, String> redisTemplate;

  public OtpSessionPortAdapter(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public OtpSessionId generateOtpSession(UUID userId) {
    OtpSessionId sessionId = OtpSessionId.generate();
    String value = userId.toString();
    redisTemplate.opsForValue().set(key(sessionId), value, SESSION_EXPIRATION);
    return sessionId;
  }

  @Override
  public Optional<UUID> findUserIdByOtpSessionId(OtpSessionId otpSessionId) {
    String userIdAsString = redisTemplate.opsForValue().get(key(otpSessionId));
    return Optional.ofNullable(userIdAsString).map(UUID::fromString);
  }

  private String key(OtpSessionId otpSessionId) {
    return SESSION_PREFIX + otpSessionId;
  }
}

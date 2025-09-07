package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpSessionPort;
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
  public String generateOtpSession(UUID userId) {
    String sessionId = UUID.randomUUID().toString();
    String key = key(sessionId);
    String value = userId.toString();
    redisTemplate.opsForValue().set(key, value, SESSION_EXPIRATION);
    return sessionId;
  }

  @Override
  public Optional<UUID> findUserIdByOtpSessionId(String otpSessionId) {
    String key = key(otpSessionId);
    String userIdAsString = redisTemplate.opsForValue().get(key);
    return Optional.ofNullable(userIdAsString).map(UUID::fromString);
  }

  private String key(String otpSessionId) {
    return SESSION_PREFIX + otpSessionId;
  }
}

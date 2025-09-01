package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenAdapter implements PasswordResetTokenPort {

  private static final String TOKEN_PREFIX = "password-reset-token:";
  private static final Duration TOKEN_EXPIRATION = Duration.ofMinutes(5);

  private final RedisTemplate<String, String> redisTemplate;

  public PasswordResetTokenAdapter(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public String save(UUID userId) {
    String token = UUID.randomUUID().toString();
    String key = TOKEN_PREFIX + token;
    redisTemplate.opsForValue().set(key, userId.toString(), TOKEN_EXPIRATION);
    return token;
  }

  @Override
  public Optional<UUID> findUserIdByResetToken(String token) {
    String userId = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
    return Optional.ofNullable(userId).map(UUID::fromString);
  }

  @Override
  public void delete(String token) {
    redisTemplate.delete(TOKEN_PREFIX + token);
  }
}

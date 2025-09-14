package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordResetTokenPort;
import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
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
  public ResetToken generateToken(UUID userId) {
    ResetToken token = ResetToken.generate();
    redisTemplate.opsForValue().set(key(token), userId.toString(), TOKEN_EXPIRATION);
    return token;
  }

  @Override
  public Optional<UUID> findUserIdByResetToken(ResetToken token) {
    String userId = redisTemplate.opsForValue().get(key(token));
    return Optional.ofNullable(userId).map(UUID::fromString);
  }

  @Override
  public void delete(ResetToken token) {
    redisTemplate.delete(key(token));
  }

  private String key(ResetToken token) {
    return TOKEN_PREFIX + token;
  }
}

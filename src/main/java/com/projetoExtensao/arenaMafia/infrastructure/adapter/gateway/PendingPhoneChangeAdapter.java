package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.user.port.gateway.PendingPhoneChangePort;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PendingPhoneChangeAdapter implements PendingPhoneChangePort {

  private static final String PHONE_CHANGE_PREFIX = "pending-phone-change:";
  private static final Duration PENDING_PHONE_CHANGE_EXPIRATION = Duration.ofMinutes(5);

  private final RedisTemplate<String, String> redisTemplate;

  public PendingPhoneChangeAdapter(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void save(UUID userId, String newPhone) {
    redisTemplate.opsForValue().set(key(userId), newPhone, PENDING_PHONE_CHANGE_EXPIRATION);
  }

  @Override
  public Optional<String> findPhoneByUserId(UUID userId) {
    String phone = redisTemplate.opsForValue().get(key(userId));
    return Optional.ofNullable(phone);
  }

  @Override
  public void deleteByUserId(UUID userId) {
    redisTemplate.delete(key(userId));
  }

  private String key(UUID userId) {
    return PHONE_CHANGE_PREFIX + userId;
  }
}

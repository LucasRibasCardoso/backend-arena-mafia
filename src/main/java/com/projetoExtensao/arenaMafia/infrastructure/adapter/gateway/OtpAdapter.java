package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOtpException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OtpAdapter implements OtpPort {

  private static final String OTP_PREFIX = "otp:user:";
  private static final Duration OTP_EXPIRATION = Duration.ofMinutes(5);

  private final RedisTemplate<String, String> redisTemplate;

  public OtpAdapter(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public String generateAndSaveOtp(UUID userId) {
    // Cria um código OTP de 6 dígitos
    String otpCode = new DecimalFormat("000000").format(new SecureRandom().nextInt(999999));

    // Salva o código OTP no Redis
    String redisKey = OTP_PREFIX + userId.toString();
    redisTemplate.opsForValue().set(redisKey, otpCode, OTP_EXPIRATION);

    return otpCode;
  }

  @Override
  public void validateOtp(UUID uuid, String otpCode) {
    String redisKey = OTP_PREFIX + uuid.toString();
    String storedOtp = redisTemplate.opsForValue().get(redisKey);

    if (storedOtp == null || !storedOtp.equals(otpCode)) {
      throw new InvalidOtpException("Código de verificação inválido ou expirado.");
    }

    redisTemplate.delete(redisKey);
  }
}

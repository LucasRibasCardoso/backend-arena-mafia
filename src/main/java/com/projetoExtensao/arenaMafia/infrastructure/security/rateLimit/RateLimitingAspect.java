package com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(
    value = "resilience4j.ratelimiter.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RateLimitingAspect {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitingAspect.class);

  private final RateLimiterRegistry rateLimiterRegistry;
  private final RateLimitKeyProvider rateLimitKeyProvider;
  private final HttpServletRequest request;

  public RateLimitingAspect(
      RateLimiterRegistry rateLimiterRegistry,
      RateLimitKeyProvider rateLimitKeyProvider,
      HttpServletRequest request) {
    this.rateLimiterRegistry = rateLimiterRegistry;
    this.rateLimitKeyProvider = rateLimitKeyProvider;
    this.request = request;
  }

  @Around("@annotation(customRateLimiter)")
  public Object rateLimit(ProceedingJoinPoint joinPoint, CustomRateLimiter customRateLimiter)
      throws Throwable {

    String limiterName = customRateLimiter.limiterName();
    RateLimiter baseRateLimiter = rateLimiterRegistry.rateLimiter(limiterName);

    String key = rateLimitKeyProvider.resolveKey(request);
    String dynamicLimiterName = limiterName + "#" + key;

    RateLimiterConfig config = baseRateLimiter.getRateLimiterConfig();
    RateLimiter rateLimiterForKey = rateLimiterRegistry.rateLimiter(dynamicLimiterName, config);

    logger.debug("Aplicando rate limit '{} para a chave '{}'", limiterName, key);

    RateLimiter.waitForPermission(rateLimiterForKey);
    return joinPoint.proceed();
  }
}

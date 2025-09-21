package com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomRateLimiter {
  /**
   * O nome da instância do RateLimiter definida no application.yml. Ex: "loginRateLimiter",
   * "sensitiveOperationLimiter".
   */
  String limiterName();
}

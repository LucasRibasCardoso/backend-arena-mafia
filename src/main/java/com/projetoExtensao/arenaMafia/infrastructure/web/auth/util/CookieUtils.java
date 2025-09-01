package com.projetoExtensao.arenaMafia.infrastructure.web.auth.util;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

  @Value("${spring.security.jwt.refresh-token-expiration-days}")
  private long refreshTokenExpirationDays;

  public ResponseCookie createRefreshTokenCookie(String refreshToken) {
    return ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/api/auth")
        .maxAge(Duration.ofDays(refreshTokenExpirationDays))
        .sameSite("Strict")
        .build();
  }

  public ResponseCookie createRefreshTokenExpiredCookie() {
    return ResponseCookie.from("refreshToken", "")
        .httpOnly(true)
        .secure(true)
        .path("/api/auth")
        .maxAge(0) // Expira imediatamente
        .sameSite("Strict")
        .build();
  }
}

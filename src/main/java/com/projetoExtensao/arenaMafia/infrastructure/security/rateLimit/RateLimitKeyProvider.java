package com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyProvider {

  public String resolveKey(HttpServletRequest request) {
    return getUsernameFromPrincipal().orElseGet(() -> getIpFromRequest(request));
  }

  private Optional<String> getUsernameFromPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {
      return Optional.ofNullable(authentication.getName());
    }
    return Optional.empty();
  }

  private String getIpFromRequest(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;

  public JwtTokenFilter(
      JwtTokenProvider tokenProvider) {

    this.tokenProvider = tokenProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = tokenProvider.resolveToken(request);

    if (isTokenValid(token)) {
      Authentication authentication = tokenProvider.getAuthentication(token);
      applyAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private boolean isTokenValid(String token) {
    return token != null && !token.isBlank() && tokenProvider.validateToken(token);
  }

  private void applyAuthentication(Authentication authentication) {
    if (authentication != null) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
  }
}

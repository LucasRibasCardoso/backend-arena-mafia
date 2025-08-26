package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.InvalidJwtTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

  private final JwtTokenProvider tokenProvider;
  private final CustomAuthenticationEntryPointHandler customAuthenticationEntryPointHandler;

  public JwtTokenFilter(
      JwtTokenProvider tokenProvider,
      CustomAuthenticationEntryPointHandler customAuthenticationEntryPointHandler) {
    this.tokenProvider = tokenProvider;
    this.customAuthenticationEntryPointHandler = customAuthenticationEntryPointHandler;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = tokenProvider.resolveToken(request);

    if (token != null) {
      try {
        Authentication authentication = tokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (InvalidJwtTokenException exception) {
        logger.warn("Tentativa de autenticação com token JWT inválido: {}", exception.getMessage());
        delegateToCustomEntryPoint(request, response, exception);
        return;
      }
    }

    // Continua a cadeia de filtros
    filterChain.doFilter(request, response);
  }

  private void delegateToCustomEntryPoint(
      HttpServletRequest request, HttpServletResponse response, Exception e) throws IOException {

    SecurityContextHolder.clearContext();
    customAuthenticationEntryPointHandler.commence(
        request, response, new InsufficientAuthenticationException(e.getMessage(), e));
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.InvalidCredentialsException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.UnauthorizedException;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenProvider;
import com.projetoExtensao.arenaMafia.infrastructure.security.userDetails.UserDetailsAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class AuthAdapter implements AuthPort {

  @Value("${spring.security.jwt.refresh-token-expiration-days}")
  private Long refreshTokenExpirationDays;

  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

  public AuthAdapter(
      JwtTokenProvider jwtTokenProvider,
      AuthenticationManager authenticationManager,
      RefreshTokenRepositoryPort refreshTokenRepositoryPort) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.authenticationManager = authenticationManager;
    this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
  }

  @Override
  public User authenticate(String username, String password) {
    try {
      var usernamePassword = new UsernamePasswordAuthenticationToken(username, password);
      Authentication authentication = authenticationManager.authenticate(usernamePassword);
      UserDetailsAdapter userDetails = (UserDetailsAdapter) authentication.getPrincipal();
      return userDetails.getUser();
    } catch (AuthenticationException e) {
      if (e.getCause() instanceof UnauthorizedException unauthorizedException) {
        throw unauthorizedException;
      }
      throw new InvalidCredentialsException();
    }
  }

  @Override
  public AuthResult generateTokens(User user) {
    refreshTokenRepositoryPort.deleteByUser(user);
    String accessToken = generateAccessToken(user);
    RefreshTokenVO refreshToken = generateRefreshToken(user);
    return new AuthResult(user, accessToken, refreshToken);
  }

  private RefreshTokenVO generateRefreshToken(User user) {
    RefreshToken refreshToken = RefreshToken.create(refreshTokenExpirationDays, user);
    RefreshToken savedRefreshToken = refreshTokenRepositoryPort.save(refreshToken);
    return savedRefreshToken.getToken();
  }

  private String generateAccessToken(User user) {
    return jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
  }
}

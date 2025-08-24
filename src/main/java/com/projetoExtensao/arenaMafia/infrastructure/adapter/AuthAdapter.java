package com.projetoExtensao.arenaMafia.infrastructure.adapter;

import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthPort;
import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthResult;
import com.projetoExtensao.arenaMafia.application.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.security.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthAdapter implements AuthPort {

  @Value("${spring.security.jwt.refresh-token-expiration-days}")
  private Long refreshTokenExpirationDays;

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

  public AuthAdapter(
      AuthenticationManager authenticationManager,
      JwtTokenProvider jwtTokenProvider,
      RefreshTokenRepositoryPort refreshTokenRepositoryPort) {
    this.authenticationManager = authenticationManager;
    this.jwtTokenProvider = jwtTokenProvider;
    this.refreshTokenRepositoryPort = refreshTokenRepositoryPort;
  }

  @Override
  public User authenticate(String username, String password) {
    var usernamePassword = new UsernamePasswordAuthenticationToken(username, password);
    Authentication authentication = authenticationManager.authenticate(usernamePassword);
    UserDetailsAdapter userDetails = (UserDetailsAdapter) authentication.getPrincipal();
    return userDetails.getUser();
  }

  @Override
  public AuthResult generateTokens(User user) {
    refreshTokenRepositoryPort.deleteByUser(user);
    String accessToken = generateAccessToken(user);
    String refreshToken = generateRefreshToken(user);
    return new AuthResult(user.getUsername(), accessToken, refreshToken);
  }

  private String generateRefreshToken(User user) {
    // Gera um novo RefreshToken
    RefreshToken refreshToken = RefreshToken.create(refreshTokenExpirationDays, user);

    // Salva o RefreshToken no banco de dados
    RefreshToken savedRefreshToken = refreshTokenRepositoryPort.save(refreshToken);

    return savedRefreshToken.getToken().toString();
  }

  private String generateAccessToken(User user) {
    return jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole());
  }
}

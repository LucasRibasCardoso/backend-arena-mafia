package com.projetoExtensao.arenaMafia.infrastructure.adapter;

import com.projetoExtensao.arenaMafia.application.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.security.UserDetailsAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenProvider;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthAdapter implements AuthPort {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthAdapter(
      AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
    this.authenticationManager = authenticationManager;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  public User authenticate(String username, String password) {
    var usernamePassword = new UsernamePasswordAuthenticationToken(username, password);
    Authentication authentication = authenticationManager.authenticate(usernamePassword);
    UserDetailsAdapter userDetails = (UserDetailsAdapter) authentication.getPrincipal();
    return userDetails.getUser();
  }

  @Override
  public TokenResponseDto getTokens(String username, RoleEnum role) {
    return jwtTokenProvider.getTokens(username, role);
  }

  @Override
  public TokenResponseDto getRefreshToken(String username, RoleEnum role) {
    return jwtTokenProvider.getTokens(username, role);
  }
}

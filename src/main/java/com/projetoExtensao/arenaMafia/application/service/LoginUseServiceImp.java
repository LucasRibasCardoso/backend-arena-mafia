package com.projetoExtensao.arenaMafia.application.service;

import com.projetoExtensao.arenaMafia.application.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.useCase.LoginUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.TokenResponseDto;

public class LoginUseServiceImp implements LoginUseCase {

  private final AuthPort authPort;

  public LoginUseServiceImp(AuthPort authPort) {
    this.authPort = authPort;
  }

  @Override
  public TokenResponseDto login(LoginRequestDto loginRequestDto) {
    User user = authPort.authenticate(loginRequestDto.username(), loginRequestDto.password());
    return authPort.createAccessToken(user.getUsername(), user.getRole());
  }
}

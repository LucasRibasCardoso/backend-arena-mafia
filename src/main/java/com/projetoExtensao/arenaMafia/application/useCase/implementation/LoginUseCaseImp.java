package com.projetoExtensao.arenaMafia.application.useCase.implementation;

import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthPort;
import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthResult;
import com.projetoExtensao.arenaMafia.application.useCase.LoginUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import org.springframework.transaction.annotation.Transactional;

public class LoginUseCaseImp implements LoginUseCase {

  private final AuthPort authPort;

  public LoginUseCaseImp(AuthPort authPort) {
    this.authPort = authPort;
  }

  @Override
  @Transactional
  public AuthResult execute(LoginRequestDto loginRequestDto) {
    // Autentica o usuário
    User user = authPort.authenticate(loginRequestDto.username(), loginRequestDto.password());
    return authPort.generateTokens(user);
  }
}

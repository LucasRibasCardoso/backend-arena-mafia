package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.LoginUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginUseCaseImp implements LoginUseCase {

  private final AuthPort authPort;

  public LoginUseCaseImp(AuthPort authPort) {
    this.authPort = authPort;
  }

  @Override
  public AuthResult execute(LoginRequestDto loginRequestDto) {
    User user = authPort.authenticate(loginRequestDto.username(), loginRequestDto.password());
    user.ensureAccountEnabled();
    return authPort.generateTokens(user);
  }
}

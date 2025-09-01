package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.authentication.LoginUseCase;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginUseCaseImp implements LoginUseCase {

  private final AuthPort authPort;
  private final UserRepositoryPort userRepository;

  public LoginUseCaseImp(AuthPort authPort, UserRepositoryPort userRepository) {
    this.authPort = authPort;
    this.userRepository = userRepository;
  }

  @Override
  public AuthResult execute(LoginRequestDto loginRequestDto) {
    User existingUser = getUserOrElseThrow(loginRequestDto.username());
    existingUser.ensureAccountEnabled();
    User user = authPort.authenticate(loginRequestDto.username(), loginRequestDto.password());
    return authPort.generateTokens(user);
  }

  private User getUserOrElseThrow(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () ->
                new BadCredentialsException(
                    "Credenciais inválidas. Por favor, verifique seu usuário e senha."));
  }
}

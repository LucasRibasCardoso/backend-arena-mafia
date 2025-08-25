package com.projetoExtensao.arenaMafia.application.auth.usecase.imp;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.LoginUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.AccountLockedException;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.AccountNotVerifiedException;
import com.projetoExtensao.arenaMafia.domain.exception.user.account.DisabledAccountException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.LoginRequestDto;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

public class LoginUseCaseImp implements LoginUseCase {

  private final AuthPort authPort;
  private final UserRepositoryPort userRepository;

  public LoginUseCaseImp(AuthPort authPort, UserRepositoryPort userRepository) {
    this.authPort = authPort;
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public AuthResult execute(LoginRequestDto loginRequestDto) {
    User existingUser =
        userRepository
            .findByUsername(loginRequestDto.username())
            .orElseThrow(
                () ->
                    new BadCredentialsException(
                        "Credenciais inválidas. Por favor, verifique seu usuário e senha."));

    // Verifica se a conta está ativa
    ensureAccountIsActive(existingUser);

    // Autentica o usuário
    User user = authPort.authenticate(loginRequestDto.username(), loginRequestDto.password());
    return authPort.generateTokens(user);
  }

  private void ensureAccountIsActive(User existingUser) {
    if (existingUser.getStatus() != AccountStatus.ACTIVE) {
      if (existingUser.getStatus().equals(AccountStatus.PENDING_VERIFICATION)) {
        throw new AccountNotVerifiedException("Sua conta ainda não foi verificada.");
      }

      if (existingUser.getStatus().equals(AccountStatus.LOCKED)) {
        throw new AccountLockedException("Sua conta está bloqueada. Por favor, contate o suporte.");
      }

      throw new DisabledAccountException("Está conta não está ativa.");
    }
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.config.beans;

import com.projetoExtensao.arenaMafia.application.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.useCase.implementation.LoginUseCaseImp;
import com.projetoExtensao.arenaMafia.application.useCase.implementation.RefreshTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.application.useCase.LoginUseCase;
import com.projetoExtensao.arenaMafia.application.useCase.RefreshTokenUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseBeans {

  @Bean
  public LoginUseCase loginUseCase(AuthPort authPort) {
    return new LoginUseCaseImp(authPort);
  }

  @Bean
  public RefreshTokenUseCase refreshTokenUseCase(
      AuthPort authPort, RefreshTokenRepositoryPort refreshTokenRepository) {
    return new RefreshTokenUseCaseImp(authPort, refreshTokenRepository);
  }
}

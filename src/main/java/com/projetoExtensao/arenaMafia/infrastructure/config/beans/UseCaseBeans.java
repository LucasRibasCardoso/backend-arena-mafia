package com.projetoExtensao.arenaMafia.infrastructure.config.beans;

import com.projetoExtensao.arenaMafia.application.port.gateway.auth.AuthPort;
import com.projetoExtensao.arenaMafia.application.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.useCase.LoginUseCase;
import com.projetoExtensao.arenaMafia.application.useCase.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.application.useCase.SignUpUseCase;
import com.projetoExtensao.arenaMafia.application.useCase.implementation.LoginUseCaseImp;
import com.projetoExtensao.arenaMafia.application.useCase.implementation.RefreshTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.application.useCase.implementation.SignUpUseCaseImp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseBeans {

  @Bean
  public LoginUseCase loginUseCase(AuthPort authPort, UserRepositoryPort userRepository) {
    return new LoginUseCaseImp(authPort, userRepository);
  }

  @Bean
  public SignUpUseCase signUpUseCase() {
    return new SignUpUseCaseImp();
  }

  @Bean
  public RefreshTokenUseCase refreshTokenUseCase(
      AuthPort authPort, RefreshTokenRepositoryPort refreshTokenRepository) {
    return new RefreshTokenUseCaseImp(authPort, refreshTokenRepository);
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.config.beans;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.LoginUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.RefreshTokenUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.SignUpUseCase;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.LoginUseCaseImp;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.RefreshTokenUseCaseImp;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.SignUpUseCaseImp;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseBeans {

  @Bean
  public LoginUseCase loginUseCase(AuthPort authPort, UserRepositoryPort userRepository) {
    return new LoginUseCaseImp(authPort, userRepository);
  }

  @Bean
  public SignUpUseCase signUpUseCase(
      PasswordEncoderPort passwordEncoderPort,
      UserRepositoryPort userRepository,
      PhoneValidatorPort phoneValidator,
      ApplicationEventPublisher eventPublisher) {
    return new SignUpUseCaseImp(
        userRepository, phoneValidator, passwordEncoderPort, eventPublisher);
  }

  @Bean
  public RefreshTokenUseCase refreshTokenUseCase(
      AuthPort authPort, RefreshTokenRepositoryPort refreshTokenRepository) {
    return new RefreshTokenUseCaseImp(authPort, refreshTokenRepository);
  }
}

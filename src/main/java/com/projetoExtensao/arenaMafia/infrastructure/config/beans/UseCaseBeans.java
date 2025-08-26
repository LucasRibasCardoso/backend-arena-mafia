package com.projetoExtensao.arenaMafia.infrastructure.config.beans;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.*;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.auth.usecase.*;
import com.projetoExtensao.arenaMafia.application.auth.usecase.imp.*;
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

  @Bean
  public VerifyAccountUseCase verifyAccountUseCase(
      AuthPort authPort, OtpPort otpPort, UserRepositoryPort userRepository) {
    return new VerifyAccountUseCaseImp(authPort, otpPort, userRepository);
  }

  @Bean
  public ResendCodeUseCase resendCodeUseCase(
      UserRepositoryPort userRepository, ApplicationEventPublisher eventPublisher) {
    return new ResendCodeUseCaseImp(userRepository, eventPublisher);
  }

  @Bean
  public LogoutUseCase logoutUseCase(RefreshTokenRepositoryPort refreshTokenRepository) {
    return new LogoutUseCaseImp(refreshTokenRepository);
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.config.beans;

import com.projetoExtensao.arenaMafia.application.gateway.AuthPort;
import com.projetoExtensao.arenaMafia.application.service.LoginUseServiceImp;
import com.projetoExtensao.arenaMafia.application.useCase.LoginUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseBeans {

  @Bean
  public LoginUseCase loginUseCase(AuthPort authPort) {
    return new LoginUseServiceImp(authPort);
  }
}

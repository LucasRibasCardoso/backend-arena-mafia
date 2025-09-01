package com.projetoExtensao.arenaMafia.infrastructure.config.bean;

import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpAdapterConfig {

  @Bean
  public SecureRandom secureRandom() {
    return new SecureRandom();
  }
}

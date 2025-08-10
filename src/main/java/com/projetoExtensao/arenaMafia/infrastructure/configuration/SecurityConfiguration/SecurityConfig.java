package com.projetoExtensao.arenaMafia.infrastructure.configuration.SecurityConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    // Desabilita configurações padrão de segurança
    http
        .headers(headers -> headers
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
        )
        .csrf(CsrfConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()
        )
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);
    return http.build();
  }
}

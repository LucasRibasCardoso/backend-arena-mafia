package com.projetoExtensao.arenaMafia.infrastructure.security;

import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.CustomAuthenticationEntryPointHandler;
import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

  private final CustomAuthenticationEntryPointHandler authenticationEntryPoint;
  private final JwtTokenFilter tokenFilter;

  public SecurityConfig(
      CustomAuthenticationEntryPointHandler authenticationEntryPoint, JwtTokenFilter tokenFilter) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.tokenFilter = tokenFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        // Habilita a permissão de frames da mesma origem
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

        // Desabilita a autenticação básica HTTP
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(CsrfConfigurer::disable)

        // Define a política de sessão como stateless
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Configura o ponto de entrada para erros de autenticação
        .exceptionHandling(
            exception -> exception.authenticationEntryPoint(authenticationEntryPoint))

        //
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/h2-console/**")
                    .permitAll()
                    .anyRequest()
                    .denyAll())

        // Adiciona o filtro JWT antes do filtro de autenticação de username e senha
        .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }
}

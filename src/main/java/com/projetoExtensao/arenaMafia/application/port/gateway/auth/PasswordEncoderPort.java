package com.projetoExtensao.arenaMafia.application.port.gateway.auth;

public interface PasswordEncoderPort {
  String encode(String password);

  boolean matches(String rawPassword, String encodedPassword);
}

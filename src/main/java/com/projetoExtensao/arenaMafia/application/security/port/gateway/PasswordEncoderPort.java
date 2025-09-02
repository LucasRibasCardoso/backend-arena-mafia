package com.projetoExtensao.arenaMafia.application.security.port.gateway;

public interface PasswordEncoderPort {
  String encode(String password);

  boolean matches(String rawPassword, String encodedPassword);
}

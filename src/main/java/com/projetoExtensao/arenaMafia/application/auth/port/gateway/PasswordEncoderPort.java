package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

public interface PasswordEncoderPort {
  String encode(String password);

  boolean matches(String rawPassword, String encodedPassword);
}

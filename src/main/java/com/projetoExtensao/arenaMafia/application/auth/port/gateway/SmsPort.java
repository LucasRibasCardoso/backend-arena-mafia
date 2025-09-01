package com.projetoExtensao.arenaMafia.application.auth.port.gateway;

public interface SmsPort {
  void send(String phone, String message);
}

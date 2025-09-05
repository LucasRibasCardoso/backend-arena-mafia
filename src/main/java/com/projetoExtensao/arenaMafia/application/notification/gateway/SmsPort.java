package com.projetoExtensao.arenaMafia.application.notification.gateway;

public interface SmsPort {
  void send(String phone, String message);
}

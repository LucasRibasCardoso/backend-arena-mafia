package com.projetoExtensao.arenaMafia.application.port.gateway;

public interface PhoneValidatorPort {
  boolean isValid(String phone);

  String formatToE164(String phone);
}

package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.projetoExtensao.arenaMafia.application.auth.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.domain.exception.user.BadPhoneNumberException;
import org.springframework.stereotype.Component;

@Component
public class PhoneValidatorAdapter implements PhoneValidatorPort {

  private final PhoneNumberUtil phoneUtil;

  public PhoneValidatorAdapter() {
    this.phoneUtil = PhoneNumberUtil.getInstance();
  }

  @Override
  public String formatToE164(String phoneNumber) {
    try {
      Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, null);

      if (!phoneUtil.isValidNumber(number)) {
        throw new BadPhoneNumberException(
            "Número de telefone inválido. Verifique o DDD e a quantidade de dígitos.");
      }

      return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (Exception e) {
      throw new BadPhoneNumberException(
          "Número de telefone inválido. Por favor, inclua o código do país e o DDD (ex: +5547988887777).");
    }
  }
}

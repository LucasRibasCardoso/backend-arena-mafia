package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.projetoExtensao.arenaMafia.application.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.domain.exception.user.BadPhoneNumberException;
import org.springframework.stereotype.Component;

@Component
public class PhoneValidatorAdapter implements PhoneValidatorPort {

  private final PhoneNumberUtil phoneUtil;

  public PhoneValidatorAdapter() {
    this.phoneUtil = PhoneNumberUtil.getInstance();
  }

  @Override
  public boolean isValid(String phoneNumber) {
    try {
      Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, null);
      return phoneUtil.isValidNumber(number);
    } catch (NumberParseException e) {
      return false;
    }
  }

  @Override
  public String formatToE164(String phoneNumber) {
    try {
      Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, null);
      return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (NumberParseException e) {
      throw new BadPhoneNumberException();
    }
  }
}

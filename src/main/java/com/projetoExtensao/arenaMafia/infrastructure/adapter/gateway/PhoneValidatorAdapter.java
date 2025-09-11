package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import org.springframework.stereotype.Component;

@Component
public class PhoneValidatorAdapter implements PhoneValidatorPort {

  private final PhoneNumberUtil phoneUtil;

  public PhoneValidatorAdapter() {
    this.phoneUtil = PhoneNumberUtil.getInstance();
  }

  @Override
  public String formatToE164(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.isBlank()) {
      throw new InvalidFormatPhoneException(ErrorCode.PHONE_REQUIRED);
    }
    try {
      Phonenumber.PhoneNumber parsedPhone = phoneUtil.parse(phoneNumber, null);
      if (!phoneUtil.isValidNumber(parsedPhone)) {
        throw new InvalidFormatPhoneException(ErrorCode.PHONE_INVALID_FORMAT);
      }
      return phoneUtil.format(parsedPhone, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (NumberParseException e) {
      throw new InvalidFormatPhoneException(ErrorCode.PHONE_INVALID_FORMAT);
    }
  }
}

package com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator
    implements ConstraintValidator<PasswordsMatch, PasswordConfirmationProvider> {

  @Override
  public boolean isValid(PasswordConfirmationProvider dto, ConstraintValidatorContext context) {
    if (dto.getPassword() == null || dto.getConfirmPassword() == null) {
      return true;
    }

    boolean passwordsMatch = dto.getPassword().equals(dto.getConfirmPassword());

    if (!passwordsMatch) {
      context.disableDefaultConstraintViolation();

      context
          .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
          .addPropertyNode("confirmPassword")
          .addConstraintViolation();
    }

    return passwordsMatch;
  }
}

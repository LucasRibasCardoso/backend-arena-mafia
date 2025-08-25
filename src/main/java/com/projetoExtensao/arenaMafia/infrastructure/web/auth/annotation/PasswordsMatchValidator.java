package com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.SignupRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator
    implements ConstraintValidator<PasswordsMatch, SignupRequestDto> {

  @Override
  public boolean isValid(SignupRequestDto dto, ConstraintValidatorContext context) {
    // Delega a validação de senha nula para outras anotações, como @NotNull ou @NotBlank
    if (dto.password() == null || dto.confirmPassword() == null) {
      return true;
    }

    boolean passwordsMatch = dto.password().equals(dto.confirmPassword());

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

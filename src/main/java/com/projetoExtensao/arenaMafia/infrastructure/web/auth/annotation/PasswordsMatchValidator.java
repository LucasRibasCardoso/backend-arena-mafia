package com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.SignupRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, SignupRequestDto> {

  @Override
  public boolean isValid(SignupRequestDto dto, ConstraintValidatorContext context) {
    return dto.password() == null || Objects.equals(dto.password(), dto.confirmPassword());
  }
}

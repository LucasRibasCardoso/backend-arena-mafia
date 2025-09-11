package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordConfirmationProvider;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordsMatch(message = "PASSWORDS_DO_NOT_MATCH")
public record SignupRequestDto(
    @NotBlank(message = "USERNAME_REQUIRED")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "USERNAME_INVALID_FORMAT")
        @Size(min = 3, max = 50, message = "USERNAME_INVALID_LENGTH")
        String username,
    @NotBlank(message = "FULL_NAME_REQUIRED")
        @Size(min = 3, max = 100, message = "FULL_NAME_INVALID_LENGTH")
        String fullName,
    @NotBlank(message = "PHONE_REQUIRED")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "PHONE_INVALID_FORMAT")
        String phone,
    @NotBlank(message = "PASSWORD_REQUIRED")
        @Size(min = 6, max = 20, message = "PASSWORD_INVALID_LENGTH")
        @Pattern(regexp = "^\\S+$", message = "PASSWORD_NO_WHITESPACE")
        String password,
    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED") String confirmPassword)
    implements PasswordConfirmationProvider {

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getConfirmPassword() {
    return confirmPassword;
  }
}

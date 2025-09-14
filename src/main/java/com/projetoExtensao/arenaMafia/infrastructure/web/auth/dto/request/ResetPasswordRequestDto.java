package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordConfirmationProvider;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordsMatch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordsMatch(message = "PASSWORDS_DO_NOT_MATCH")
public record ResetPasswordRequestDto(
    @NotNull(message = "RESET_TOKEN_REQUIRED") @Valid ResetToken passwordResetToken,
    @NotBlank(message = "PASSWORD_REQUIRED")
        @Size(min = 6, max = 20, message = "PASSWORD_INVALID_LENGTH")
        @Pattern(regexp = "^\\S+$", message = "PASSWORD_NO_WHITESPACE")
        String newPassword,
    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
        @Size(min = 6, max = 20, message = "PASSWORD_INVALID_LENGTH")
        @Pattern(regexp = "^\\S+$", message = "PASSWORD_NO_WHITESPACE")
        String confirmPassword)
    implements PasswordConfirmationProvider {

  @Override
  public String getPassword() {
    return newPassword;
  }

  @Override
  public String getConfirmPassword() {
    return confirmPassword;
  }
}

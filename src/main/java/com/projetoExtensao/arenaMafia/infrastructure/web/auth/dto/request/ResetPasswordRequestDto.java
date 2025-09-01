package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordConfirmationProvider;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordsMatch(message = "A senha de confirmação não corresponde à nova senha.")
public record ResetPasswordRequestDto(
    @NotBlank(message = "O token de redefinição de senha é obrigatório.") String passwordResetToken,
    @NotBlank(message = "Por favor, insira uma senha.")
        @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres.")
        @Pattern(regexp = "^\\S+$", message = "A senha não pode conter espaços em branco.")
        String newPassword,
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

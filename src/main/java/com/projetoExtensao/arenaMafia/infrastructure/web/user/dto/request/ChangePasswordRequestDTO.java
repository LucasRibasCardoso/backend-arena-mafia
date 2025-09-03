package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordConfirmationProvider;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordsMatch(message = "A senha de confirmação não corresponde à senha.")
public record ChangePasswordRequestDTO(
    @NotBlank(message = "Por favor, insira a sua senha atual.")
        @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres.")
        @Pattern(regexp = "^\\S+$", message = "A senha não pode conter espaços em branco.")
        String currentPassword,
    @NotBlank(message = "Por favor, insira uma nova senha.")
        @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres.")
        @Pattern(regexp = "^\\S+$", message = "A senha não pode conter espaços em branco.")
        String newPassword,
    @NotBlank(message = "Por favor, confirme sua nova senha.") String confirmPassword)
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

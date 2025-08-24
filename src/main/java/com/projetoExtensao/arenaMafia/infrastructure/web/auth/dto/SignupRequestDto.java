package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.annotation.PasswordsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordsMatch(message = "A senha e a confirmação de senha não conferem.")
public record SignupRequestDto(

    @NotBlank(message = "Por favor, informe um nome de usuário.")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "O nome de usuário deve conter apenas letras, números e underscore (_).")
    @Size(min = 3, max = 20, message = "O nome de usuário deve ter entre 3 e 20 caracteres.")
    String username,

    @NotBlank(message = "Por favor, informe seu nome completo.")
    String fullName,

    @NotBlank(message = "Por favor, informe seu telefone.")
    @Pattern(regexp = "^\\+\\d{10,15}$", message = "O telefone deve estar no formato internacional (ex: +5547999999999).")
    String phone,

    @NotBlank(message = "Por favor, insira uma senha.")
    @Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres.")
    String password,

    // Não precisa de annotation, pois a sua comparação é feita pela annotation @PasswordsMatch
    String confirmPassword) {}

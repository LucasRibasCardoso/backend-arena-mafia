package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequestDto(
    @NotBlank(message = "O ID da sessão OTP não pode estar vazio.") String otpSessionId) {}

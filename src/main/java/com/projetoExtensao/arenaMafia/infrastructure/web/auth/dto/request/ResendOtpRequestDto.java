package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import jakarta.validation.constraints.NotNull;

public record ResendOtpRequestDto(
    @NotNull(message = "O ID da sessão OTP não pode estar vazio.") OtpSessionId otpSessionId) {}

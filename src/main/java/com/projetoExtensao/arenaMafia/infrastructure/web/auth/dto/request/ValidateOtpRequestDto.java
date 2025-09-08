package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ValidateOtpRequestDto(
    @NotNull(message = "O ID da sessão OTP não pode estar vazio.") @Valid OtpSessionId otpSessionId,
    @NotNull(message = "O código de verificação é obrigatório.") @Valid OtpCode otpCode) {}

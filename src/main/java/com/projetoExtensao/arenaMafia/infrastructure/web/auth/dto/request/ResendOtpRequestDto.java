package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ResendOtpRequestDto(
    @NotNull(message = "OTP_SESSION_ID_REQUIRED") @Valid OtpSessionId otpSessionId) {}

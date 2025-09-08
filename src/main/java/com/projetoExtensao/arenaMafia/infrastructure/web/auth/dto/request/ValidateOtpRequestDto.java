package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ValidateOtpRequestDto(
    @NotNull(message = "OTP_SESSION_ID_REQUIRED") @Valid OtpSessionId otpSessionId,
    @NotNull(message = "OTP_CODE_REQUIRED") @Valid OtpCode otpCode) {}

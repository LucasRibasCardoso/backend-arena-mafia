package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CompletePhoneChangeRequestDto(
    @NotNull(message = "OTP_CODE_REQUIRED") @Valid OtpCode otpCode) {}

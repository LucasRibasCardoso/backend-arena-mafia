package com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import jakarta.validation.constraints.NotNull;

public record CompletePhoneChangeRequestDto(
    @NotNull(message = "O código de verificação é obrigatório.") OtpCode otpCode) {}

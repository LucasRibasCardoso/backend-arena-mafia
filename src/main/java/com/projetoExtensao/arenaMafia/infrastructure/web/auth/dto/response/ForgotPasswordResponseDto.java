package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;

public record ForgotPasswordResponseDto(OtpSessionId otpSessionId, String message) {}

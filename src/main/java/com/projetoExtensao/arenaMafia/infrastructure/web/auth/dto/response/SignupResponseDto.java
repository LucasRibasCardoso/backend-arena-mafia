package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response;

import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpSessionId;

public record SignupResponseDto(OtpSessionId otpSessionId, String message) {}

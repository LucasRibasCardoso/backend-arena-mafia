package com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response;

import com.projetoExtensao.arenaMafia.domain.valueobjects.ResetToken;

public record PasswordResetTokenResponseDto(ResetToken passwordResetToken) {}

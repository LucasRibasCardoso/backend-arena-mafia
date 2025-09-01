package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;

public interface VerifyAccountUseCase {
  AuthResult execute(ValidateOtpRequestDto requestDto);
}

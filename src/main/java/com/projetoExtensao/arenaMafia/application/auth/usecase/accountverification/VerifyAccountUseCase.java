package com.projetoExtensao.arenaMafia.application.auth.usecase.accountverification;

import com.projetoExtensao.arenaMafia.application.auth.model.AuthResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ValidateOtpRequestDto;

public interface VerifyAccountUseCase {
  AuthResult execute(ValidateOtpRequestDto request);
}

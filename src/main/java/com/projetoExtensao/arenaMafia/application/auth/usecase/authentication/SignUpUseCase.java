package com.projetoExtensao.arenaMafia.application.auth.usecase.authentication;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;

public interface SignUpUseCase {
  User execute(SignupRequestDto requestDto);
}

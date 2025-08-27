package com.projetoExtensao.arenaMafia.application.auth.usecase;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.SignupRequestDto;

public interface SignUpUseCase {
  String execute(SignupRequestDto requestDto);
}

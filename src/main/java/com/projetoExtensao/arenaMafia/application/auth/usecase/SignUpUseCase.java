package com.projetoExtensao.arenaMafia.application.auth.usecase;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.SignupRequestDto;

public interface SignUpUseCase {
  String execute(SignupRequestDto requestDto);
}

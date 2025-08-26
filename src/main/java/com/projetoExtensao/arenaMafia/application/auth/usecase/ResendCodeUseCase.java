package com.projetoExtensao.arenaMafia.application.auth.usecase;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.ResendCodeRequestDto;

public interface ResendCodeUseCase {
  void execute(ResendCodeRequestDto requestDto);
}

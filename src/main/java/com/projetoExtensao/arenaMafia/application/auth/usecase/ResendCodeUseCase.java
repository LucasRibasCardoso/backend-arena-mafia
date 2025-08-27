package com.projetoExtensao.arenaMafia.application.auth.usecase;

import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.ResendCodeRequestDto;

public interface ResendCodeUseCase {
  void execute(ResendCodeRequestDto requestDto);
}

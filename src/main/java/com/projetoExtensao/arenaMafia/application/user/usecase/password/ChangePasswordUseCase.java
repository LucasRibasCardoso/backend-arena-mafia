package com.projetoExtensao.arenaMafia.application.user.usecase.password;

import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangePasswordRequestDto;
import java.util.UUID;

public interface ChangePasswordUseCase {
  void execute(UUID idCurrentUser, ChangePasswordRequestDto request);
}

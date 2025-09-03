package com.projetoExtensao.arenaMafia.application.user.usecase.password;

import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangePasswordRequestDTO;
import java.util.UUID;

public interface ChangePasswordUseCase {
  void execute(UUID idCurrentUser, ChangePasswordRequestDTO request);
}

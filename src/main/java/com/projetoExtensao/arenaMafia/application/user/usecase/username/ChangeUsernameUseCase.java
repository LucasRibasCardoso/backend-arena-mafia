package com.projetoExtensao.arenaMafia.application.user.usecase.username;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangeUsernameDTO;
import java.util.UUID;

public interface ChangeUsernameUseCase {
  User execute(UUID idCurrentUser, ChangeUsernameDTO request);
}

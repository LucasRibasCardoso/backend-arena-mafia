package com.projetoExtensao.arenaMafia.application.user.usecase.username;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.ChangeUsernameRequestDTO;
import java.util.UUID;

public interface ChangeUsernameUseCase {
  User execute(UUID idCurrentUser, ChangeUsernameRequestDTO request);
}

package com.projetoExtensao.arenaMafia.application.user.usecase.profile;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.UpdateProfileRequestDTO;
import java.util.UUID;

public interface UpdateProfileUseCase {
  User execute(UUID idCurrentUser, UpdateProfileRequestDTO request);
}

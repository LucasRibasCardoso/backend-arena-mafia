package com.projetoExtensao.arenaMafia.application.user.usecase.profile;

import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.UUID;

public interface GetUserProfileUseCase {
  User execute(UUID userId);
}

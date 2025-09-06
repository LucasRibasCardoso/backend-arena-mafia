package com.projetoExtensao.arenaMafia.application.user.usecase.disable;

import java.util.UUID;

public interface DisableMyAccountUseCase {
  void execute(UUID idCurrentUser);
}

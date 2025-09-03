package com.projetoExtensao.arenaMafia.application.user.usecase.phone;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.web.user.dto.request.CompletePhoneChangeRequestDTO;
import java.util.UUID;

public interface CompleteChangePhoneUseCase {
  User execute(UUID idCurrentUser, CompletePhoneChangeRequestDTO request);
}
